package org.smallmind.persistence.orm.hibernate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.model.reflect.ReflectionUtility;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.WaterfallORMDao;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

public abstract class HibernateDao<D extends Durable<Long>> extends WaterfallORMDao<Long, D> {

   private HibernateProxySession proxySession;

   public HibernateDao (HibernateProxySession proxySession) {

      this(proxySession, null);
   }

   public HibernateDao (HibernateProxySession proxySession, VectoredDao<Long, D> vectoredDao) {

      super(vectoredDao, proxySession.willAllowCascade());

      this.proxySession = proxySession;
   }

   public void register () {

      DaoManager.register(getManagedClass(), this);
   }

   public String getDataSource () {

      return proxySession.getDataSource();
   }

   public ProxySession getSession () {

      return proxySession;
   }

   public Class<Long> getIdClass () {

      return Long.class;
   }

   public Long getIdFromString (String value) {

      return Long.parseLong(value);
   }

   public Long getId (D durable) {

      return durable.getId();
   }

   public D get (Long id) {

      return get(getManagedClass(), id);
   }

   public D get (Class<D> durableClass, Long id) {

      D durable;
      Object persistedObject;
      VectoredDao<Long, D> nextDao = getNextDao();

      if (nextDao != null) {
         if ((durable = nextDao.get(durableClass, id)) != null) {

            return durable;
         }
      }

      if ((persistedObject = proxySession.getSession().get(durableClass, id)) != null) {
         durable = durableClass.cast(persistedObject);

         if (nextDao != null) {

            return nextDao.persist(durableClass, durable);
         }

         return durable;
      }

      return null;
   }

   public List<D> list () {

      return Collections.checkedList(proxySession.getSession().createCriteria(getManagedClass()).list(), getManagedClass());
   }

   public Iterable<D> scroll (int fetchSize) {

      return new ScrollIterator<D>(proxySession.getSession().createCriteria(getManagedClass()).setFetchSize(fetchSize).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   public Iterable<D> scrollById (final Long greaterThan, final int fetchSize) {

      return scrollByCriteria(new CriteriaDetails() {

         @Override
         public Criteria completeCriteria (Criteria criteria) {

            return criteria.add(Restrictions.gt("id", greaterThan)).addOrder(Order.asc("id")).setFetchSize(fetchSize);
         }
      });
   }

   public long size () {

      return findByCriteria(Integer.class, new CriteriaDetails() {

         @Override
         public Criteria completeCriteria (Criteria criteria) {

            return criteria.setProjection(Projections.rowCount());
         }
      });
   }

   public void imprint (D durable) {

      proxySession.getSession().merge(durable);
   }

   public D persist (D durable) {

      return persist(getManagedClass(), durable);
   }

   public D persist (Class<D> durableClass, D durable) {

      D persistentDurable;
      VectoredDao<Long, D> nextDao = getNextDao();

      if (proxySession.getSession().contains(durable)) {
         persistentDurable = durable;
      }
      else {
         persistentDurable = getManagedClass().cast(proxySession.getSession().merge(durable));
      }

      if (nextDao != null) {

         return nextDao.persist(durableClass, persistentDurable);
      }

      return persistentDurable;
   }

   public void delete (D durable) {

      delete(getManagedClass(), durable);
   }

   public void delete (Class<D> durableClass, D durable) {

      VectoredDao<Long, D> nextDao = getNextDao();

      if (!proxySession.getSession().contains(durable)) {
         proxySession.getSession().delete(proxySession.getSession().load(durable.getClass(), durable.getId()));
      }
      else {
         proxySession.getSession().delete(durable);
      }

      if (nextDao != null) {

         nextDao.delete(durableClass, durable);
      }
   }

   public D detach (D object) {

      throw new UnsupportedOperationException("Hibernate has no explicit detached state");
   }

   public D findBySQLQuery (SQLQueryDetails sqlQueryDetails) {

      return getManagedClass().cast(constructSQLQuery(sqlQueryDetails).addEntity(getManagedClass()).uniqueResult());
   }

   public <T> T findBySQLQuery (Class<T> returnType, SQLQueryDetails sqlQueryDetails) {

      SQLQuery sqlQuery;

      sqlQuery = constructSQLQuery(sqlQueryDetails);

      if (Durable.class.isAssignableFrom(returnType)) {

         return returnType.cast(sqlQuery.addEntity(returnType).uniqueResult());
      }
      else if (!ReflectionUtility.isEssentiallyPrimitive(returnType)) {

         return returnType.cast(sqlQuery.setResultTransformer(Transformers.aliasToBean(returnType)).uniqueResult());
      }
      else {

         Object obj;

         if ((obj = sqlQuery.uniqueResult()) != null) {

            return returnType.cast(obj);
         }

         return null;
      }
   }

   public List<D> listBySQLQuery (SQLQueryDetails sqlQueryDetails) {

      return Collections.checkedList(constructSQLQuery(sqlQueryDetails).addEntity(getManagedClass()).list(), getManagedClass());
   }

   public <T> List<T> listBySQLQuery (Class<T> returnType, SQLQueryDetails sqlQueryDetails) {

      SQLQuery sqlQuery;

      sqlQuery = constructSQLQuery(sqlQueryDetails);

      if (Durable.class.isAssignableFrom(returnType)) {

         return Collections.checkedList(sqlQuery.addEntity(returnType).list(), returnType);
      }
      else if (!ReflectionUtility.isEssentiallyPrimitive(returnType)) {

         return Collections.checkedList(sqlQuery.setResultTransformer(Transformers.aliasToBean(returnType)).list(), returnType);
      }
      else {

         LinkedList<T> returnList = new LinkedList<T>();

         for (Object obj : sqlQuery.list()) {
            returnList.add(returnType.cast(obj));
         }

         return returnList;
      }
   }

   public Iterable<D> scrollBySQLQuery (SQLQueryDetails sqlQueryDetails) {

      return new ScrollIterator<D>(constructSQLQuery(sqlQueryDetails).addEntity(getManagedClass()).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   public int executeWithQuery (QueryDetails queryDetails) {

      return constructQuery(queryDetails).executeUpdate();
   }

   public <T> T findByQuery (Class<T> returnType, QueryDetails queryDetails) {

      return returnType.cast(constructQuery(queryDetails).uniqueResult());
   }

   public D findByQuery (QueryDetails queryDetails) {

      return getManagedClass().cast(constructQuery(queryDetails).uniqueResult());
   }

   public <T> List<T> listByQuery (Class<T> returnType, QueryDetails queryDetails) {

      return Collections.checkedList(constructQuery(queryDetails).list(), returnType);
   }

   public List<D> listByQuery (QueryDetails queryDetails) {

      return Collections.checkedList(constructQuery(queryDetails).list(), getManagedClass());
   }

   public <T> Iterable<T> scrollByQuery (Class<T> returnType, QueryDetails queryDetails) {

      return new ScrollIterator<T>(constructQuery(queryDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), returnType);
   }

   public Iterable<D> scrollByQuery (QueryDetails queryDetails) {

      return new ScrollIterator<D>(constructQuery(queryDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   public <T> T findByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

      return returnType.cast(constructCriteria(criteriaDetails).uniqueResult());
   }

   public D findByCriteria (CriteriaDetails criteriaDetails) {

      return getManagedClass().cast(constructCriteria(criteriaDetails).uniqueResult());
   }

   public <T> List<T> listByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

      return Collections.checkedList(constructCriteria(criteriaDetails).list(), returnType);
   }

   public List<D> listByCriteria (CriteriaDetails criteriaDetails) {

      return Collections.checkedList(constructCriteria(criteriaDetails).list(), getManagedClass());
   }

   public <T> Iterable<T> scrollByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

      return new ScrollIterator<T>(constructCriteria(criteriaDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), returnType);
   }

   public Iterable<D> scrollByCriteria (CriteriaDetails criteriaDetails) {

      return new ScrollIterator<D>(constructCriteria(criteriaDetails).scroll(ScrollMode.SCROLL_INSENSITIVE), getManagedClass());
   }

   public SQLQuery constructSQLQuery (SQLQueryDetails sqlQueryDetails) {

      return sqlQueryDetails.completeSQLQuery((SQLQuery)proxySession.getSession().createSQLQuery(sqlQueryDetails.getSQLQueryString()).setCacheable(true));
   }

   public Query constructQuery (QueryDetails queryDetails) {

      return queryDetails.completeQuery(proxySession.getSession().createQuery(queryDetails.getQueryString()).setCacheable(true));
   }

   public Criteria constructCriteria (CriteriaDetails criteriaDetails) {

      Criteria criteria;

      criteria = (criteriaDetails.getAlias() == null) ? proxySession.getSession().createCriteria(getManagedClass()) : proxySession.getSession().createCriteria(getManagedClass(), criteriaDetails.getAlias());

      return criteriaDetails.completeCriteria(criteria).setCacheable(true);
   }

   public DetachedCriteria detachCriteria () {

      return DetachedCriteria.forClass(getManagedClass());
   }
}
