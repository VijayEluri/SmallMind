package org.smallmind.persistence.orm.jdo;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jdo.Query;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.WaterfallORMDao;

public abstract class JDODao<I extends Serializable, D extends Durable<I>> extends WaterfallORMDao<I, D> {

   private JDOProxySession proxySession;

   public JDODao (JDOProxySession proxySession) {

      this(proxySession, null);
   }

   public JDODao (JDOProxySession proxySession, VectoredDao<I, D> vectoredDao) {

      super(vectoredDao);

      this.proxySession = proxySession;
   }

   public I getId (D object) {

      return getIdClass().cast(proxySession.getPersistenceManager().getObjectId(object));
   }

   public D get (I id) {

      return get(getManagedClass(), id);
   }

   public D get (Class<D> durableClass, I id) {

      D durable;
      VectoredDao<I, D> nextDao = getNextDao();

      if (nextDao != null) {
         if ((durable = nextDao.get(durableClass, id)) != null) {

            return durable;
         }
      }

      if ((durable = durableClass.cast(proxySession.getPersistenceManager().getObjectId(id))) != null) {
         if (nextDao != null) {

            return nextDao.persist(durableClass, durable);
         }

         return durable;
      }

      return null;
   }

   public List<D> list () {

      LinkedList<D> instanceList;
      Iterator instanceIter;

      instanceIter = proxySession.getPersistenceManager().getExtent(getManagedClass()).iterator();
      instanceList = new LinkedList<D>();
      while (instanceIter.hasNext()) {
         instanceList.add(getManagedClass().cast(instanceIter.next()));
      }

      return instanceList;
   }

   public Iterable<D> scroll () {

      return new IterableIterator<D>(proxySession.getPersistenceManager().getExtent(getManagedClass()).iterator());
   }

   public D detach (D object) {

      return getManagedClass().cast(proxySession.getPersistenceManager().detachCopy(object));
   }

   public D persist (D durable) {

      return persist(getManagedClass(), durable);
   }

   public D persist (Class<D> durableClass, D durable) {

      D persistentDurable;
      VectoredDao<I, D> nextDao = getNextDao();

      persistentDurable = durableClass.cast(proxySession.getPersistenceManager().makePersistent(durable));

      if (nextDao != null) {
         return nextDao.persist(durableClass, persistentDurable);
      }

      return persistentDurable;
   }

   public void delete (D durable) {

      delete(getManagedClass(), durable);
   }

   public void delete (Class<D> durableClass, D durable) {

      VectoredDao<I, D> nextDao = getNextDao();

      proxySession.getPersistenceManager().deletePersistent(durable);

      if (nextDao != null) {
         nextDao.delete(durableClass, durable);
      }
   }

   public D find (QueryDetails queryDetails) {

      Query query;

      query = constructQuery(queryDetails);
      query.setUnique(true);

      return getManagedClass().cast(query.executeWithMap(queryDetails.getParameterMap()));
   }

   public List<D> list (QueryDetails queryDetails) {

      return Collections.checkedList((List<D>)constructQuery(queryDetails).executeWithMap(queryDetails.getParameterMap()), getManagedClass());
   }

   private Query constructQuery (QueryDetails queryDetails) {

      Query query;
      Class[] importClasses;

      query = proxySession.getPersistenceManager().newQuery(queryDetails.getQuery());
      query.setIgnoreCache(queryDetails.getIgnoreCache());

      if ((importClasses = queryDetails.getImports()) != null) {
         if (importClasses.length > 0) {

            StringBuilder importBuilder;

            importBuilder = new StringBuilder("import ");
            for (int count = 0; count < importClasses.length; count++) {
               if (count > 0) {
                  importBuilder.append("; ");
               }

               importBuilder.append(importClasses[count].getCanonicalName());
            }

            query.declareImports(importBuilder.toString());
         }
      }

      return query;
   }
}
