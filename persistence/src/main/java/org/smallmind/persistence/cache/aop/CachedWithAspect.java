/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.aop;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.VectorAwareORMDao;

@Aspect
public class CachedWithAspect {

  private static final ConcurrentHashMap<MethodKey, Method> METHOD_MAP = new ConcurrentHashMap<MethodKey, Method>();

  @Around(value = "execution(* persist (org.smallmind.persistence.Durable)) && @within(CachedWith) && this(ormDao)", argNames = "thisJoinPoint, ormDao")
  public Object aroundPersistMethod (ProceedingJoinPoint thisJoinPoint, VectorAwareORMDao ormDao)
    throws Throwable {

    CachedWith cachedWith;
    VectoredDao vectoredDao;

    if (((vectoredDao = ormDao.getVectoredDao()) == null) || ((cachedWith = ormDao.getClass().getAnnotation(CachedWith.class)) == null)) {

      return thisJoinPoint.proceed();
    }
    else {

      Durable durable;

      durable = (Durable)thisJoinPoint.proceed();

      for (Update update : cachedWith.updates()) {
        if (executeFilter(update.filter(), ormDao, durable)) {

          OnPersist onPersist = executeOnPersist(update.onPersist(), ormDao, durable);
          Operand operand = executeProxy(update.proxy(), ormDao, durable);

          switch (onPersist) {
            case INSERT:
              vectoredDao.updateInVector(new VectorKey(VectorIndices.getVectorIndexes(update.value(), durable, ormDao), operand.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), operand.getDurable());
              break;
            case REMOVE:
              vectoredDao.removeFromVector(new VectorKey(VectorIndices.getVectorIndexes(update.value(), durable, ormDao), operand.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), operand.getDurable());
              break;
            default:
              throw new UnknownSwitchCaseException(onPersist.name());
          }
        }
      }

      for (Finder finder : cachedWith.finders()) {
        if (executeFilter(finder.filter(), ormDao, durable)) {

          Iterable<Durable> finderIterable;

          if ((finderIterable = executeFinder(finder, ormDao, durable)) != null) {

            OnPersist onPersist = executeOnPersist(finder.onPersist(), ormDao, durable);
            Operand operand = executeProxy(finder.proxy(), ormDao, durable);

            for (Durable indexingDurable : finderIterable) {
              switch (onPersist) {
                case INSERT:
                  vectoredDao.updateInVector(new VectorKey(VectorIndices.getVectorIndexes(finder.vector(), indexingDurable, ormDao), operand.getManagedClass(), Classifications.get(CachedWith.class, null, finder.vector())), operand.getDurable());
                  break;
                case REMOVE:
                  vectoredDao.removeFromVector(new VectorKey(VectorIndices.getVectorIndexes(finder.vector(), indexingDurable, ormDao), operand.getManagedClass(), Classifications.get(CachedWith.class, null, finder.vector())), operand.getDurable());
                  break;
                default:
                  throw new UnknownSwitchCaseException(onPersist.name());
              }
            }
          }
        }
      }

      for (Invalidate invalidate : cachedWith.invalidators()) {
        if (executeFilter(invalidate.filter(), ormDao, durable)) {
          vectoredDao.deleteVector(new VectorKey(VectorIndices.getVectorIndexes(invalidate.vector(), durable, ormDao), invalidate.against(), Classifications.get(CachedWith.class, null, invalidate.vector())));
        }
      }

      return durable;
    }
  }

  @Around(value = "execution(void delete (..)) && @within(CachedWith) && args(durable) && this(ormDao)", argNames = "thisJoinPoint, ormDao, durable")
  public void aroundDeleteMethod (ProceedingJoinPoint thisJoinPoint, VectorAwareORMDao ormDao, Durable durable)
    throws Throwable {

    CachedWith cachedWith;
    VectoredDao vectoredDao;

    if (((vectoredDao = ormDao.getVectoredDao()) == null) || ((cachedWith = ormDao.getClass().getAnnotation(CachedWith.class)) == null)) {

      thisJoinPoint.proceed();
    }
    else {
      thisJoinPoint.proceed();

      for (Update update : cachedWith.updates()) {
        if (executeFilter(update.filter(), ormDao, durable)) {

          Operand operand = executeProxy(update.proxy(), ormDao, durable);

          vectoredDao.removeFromVector(new VectorKey(VectorIndices.getVectorIndexes(update.value(), durable, ormDao), operand.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), operand.getDurable());
        }
      }

      for (Finder finder : cachedWith.finders()) {
        if (executeFilter(finder.filter(), ormDao, durable)) {

          Iterable<Durable> finderIterable;

          if ((finderIterable = executeFinder(finder, ormDao, durable)) != null) {

            Operand operand = executeProxy(finder.proxy(), ormDao, durable);

            for (Durable indexingDurable : finderIterable) {
              vectoredDao.removeFromVector(new VectorKey(VectorIndices.getVectorIndexes(finder.vector(), indexingDurable, ormDao), operand.getManagedClass(), Classifications.get(CachedWith.class, null, finder.vector())), operand.getDurable());
            }
          }
        }
      }

      for (Invalidate invalidate : cachedWith.invalidators()) {
        if (executeFilter(invalidate.filter(), ormDao, durable)) {
          vectoredDao.deleteVector(new VectorKey(VectorIndices.getVectorIndexes(invalidate.vector(), durable, ormDao), invalidate.against(), Classifications.get(CachedWith.class, null, invalidate.vector())));
        }
      }
    }
  }

  private boolean executeFilter (String filterMethodName, VectorAwareORMDao ormDao, Durable durable) {

    Method filterMethod;

    if (filterMethodName.length() > 0) {

      if ((filterMethod = locateMethod(ormDao, filterMethodName, ormDao.getManagedClass())) == null) {
        throw new CacheAutomationError("The filter Method(%s) referenced within @CachedWith does not exist", filterMethodName);
      }

      if (!(filterMethod.getReturnType().equals(boolean.class) || filterMethod.getReturnType().equals(Boolean.class))) {
        throw new CacheAutomationError("A filter Method(%s) referenced by @CachedWith must return a value of type 'boolean'", filterMethodName);
      }

      try {

        return (Boolean)filterMethod.invoke(ormDao, durable);
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return true;
  }

  private OnPersist executeOnPersist (String onPersistMethodName, VectorAwareORMDao ormDao, Durable durable) {

    Method onPersistMethod;

    if (onPersistMethodName.length() > 0) {

      if ((onPersistMethod = locateMethod(ormDao, onPersistMethodName, ormDao.getManagedClass())) == null) {
        throw new CacheAutomationError("The onPersist Method(%s) referenced within @CachedWith does not exist", onPersistMethodName);
      }

      if (!onPersistMethod.getReturnType().equals(OnPersist.class)) {
        throw new CacheAutomationError("An onPersist Method(%s) referenced by @CachedWith must return a value of type 'OnPersist'", onPersistMethodName);
      }

      try {

        return (OnPersist)onPersistMethod.invoke(ormDao, durable);
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return OnPersist.INSERT;
  }

  private Operand executeProxy (Proxy proxy, VectorAwareORMDao ormDao, Durable durable) {

    ORMDao proxyDao;
    Method proxyGetMethod;
    Durable proxyDurable;
    Object proxyId;

    if (proxy.on().length() > 0) {

      if ((proxyDao = DaoManager.get(proxy.with())) == null) {
        throw new CacheAutomationError("Unable to locate an implementation of ORMDao within DaoManager for the requested proxy(%s)", proxy.with().getName());
      }

      proxyId = VectorIndices.getValue(durable, (!proxy.type().equals(Nothing.class)) ? proxy.type() : proxyDao.getIdClass(), proxy.on());

      if ((proxyGetMethod = locateMethod(proxyDao, "get", proxy.with())) == null) {
        throw new CacheAutomationError("The 'get(%s)' method does not exist on the ORMDao(%s) for the requested proxy", proxyDao.getIdClass(), proxyDao.getClass().getName());
      }

      try {
        if ((proxyDurable = (Durable)proxyGetMethod.invoke(proxyDao, proxyId)) == null) {
          throw new CacheAutomationError("No proxy object could be obtained via the 'get(%s)' method on the ORMDao(%s)", proxyId, proxyDao.getClass().getName());
        }

        return new Operand(proxyDao.getManagedClass(), proxyDurable);
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return new Operand(ormDao.getManagedClass(), durable);
  }

  private Iterable<Durable> executeFinder (Finder finder, VectorAwareORMDao ormDao, Durable durable) {

    Method finderMethod;
    Type finderReturnType;

    if (finder.vector().value().length > 1) {
      throw new CacheAutomationError("Finder methods are currently not compatable with Vectors that have more than a single Index");
    }

    if ((finderMethod = locateMethod(ormDao, finder.method(), ormDao.getManagedClass())) == null) {
      throw new CacheAutomationError("Method(%s) referenced by @Finder does not exist", finder.method());
    }

    if (!Iterable.class.isAssignableFrom(finderMethod.getReturnType())) {
      throw new CacheAutomationError("A Method(%s) referenced by @Finder must return a value of type <? extends Iterable>", finder.method());
    }

    if ((!((finderReturnType = finderMethod.getGenericReturnType()) instanceof ParameterizedType)) || (!((Class)((ParameterizedType)finderReturnType).getActualTypeArguments()[0]).isAssignableFrom(finder.vector().value()[0].with()))) {
      throw new CacheAutomationError("A Method(%s) referenced by @Finder must return a value of type <? extends Iterable<%s>>", finder.method(), finder.vector().value()[0].with().getSimpleName());
    }

    try {

      return (Iterable<Durable>)finderMethod.invoke(ormDao, durable);
    }
    catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }

  private Method locateMethod (ORMDao ormDao, String methodName, Class parameterType) {

    Method aspectMethod;
    MethodKey methodKey;
    Class[] parameterTypes;

    if ((aspectMethod = METHOD_MAP.get(methodKey = new MethodKey(ormDao.getClass(), methodName))) == null) {
      for (Method method : ormDao.getClass().getMethods()) {
        if (method.getName().equals(methodName) && ((parameterTypes = method.getParameterTypes()).length == 1) && (parameterTypes[0].isAssignableFrom(parameterType))) {
          METHOD_MAP.put(methodKey, aspectMethod = method);
          break;
        }
      }
    }

    return aspectMethod;
  }

  public class Operand {

    private Class<? extends Durable> managedClass;
    private Durable durable;

    private Operand (Class<? extends Durable> managedClass, Durable durable) {

      this.managedClass = managedClass;
      this.durable = durable;
    }

    public Class<? extends Durable> getManagedClass () {

      return managedClass;
    }

    public Durable getDurable () {

      return durable;
    }
  }

  public class MethodKey {

    private Class methodClass;
    private String methodName;

    private MethodKey (Class methodClass, String methodName) {

      this.methodClass = methodClass;
      this.methodName = methodName;
    }

    public Class getMethodClass () {

      return methodClass;
    }

    public String getMethodName () {

      return methodName;
    }

    @Override
    public int hashCode () {

      return methodClass.hashCode() ^ methodName.hashCode();
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MethodKey) && methodClass.equals(((MethodKey)obj).getMethodClass()) && methodName.equals(((MethodKey)obj).getMethodName());
    }
  }
}