/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
import org.smallmind.nutsnbolts.util.SingleItemIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableDao;
import org.smallmind.persistence.VectorAwareDurableDao;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.VectoredDao;

@Aspect
public class CachedWithAspect {

  private static final ConcurrentHashMap<MethodKey, Method> METHOD_MAP = new ConcurrentHashMap<MethodKey, Method>();

  @Around(value = "execution(* persist (org.smallmind.persistence.Durable+)) && @within(CachedWith) && this(durableDao)", argNames = "thisJoinPoint, durableDao")
  public Object aroundPersistMethod (ProceedingJoinPoint thisJoinPoint, VectorAwareDurableDao durableDao)
    throws Throwable {

    CachedWith cachedWith;
    VectoredDao vectoredDao;

    if (((vectoredDao = durableDao.getVectoredDao()) == null) || ((cachedWith = durableDao.getClass().getAnnotation(CachedWith.class)) == null)) {

      return thisJoinPoint.proceed();
    }
    else {

      Durable durable;

      durable = (Durable)thisJoinPoint.proceed();

      for (Update update : cachedWith.updates()) {
        if (executeFilter(update.filter(), durableDao, durable)) {

          OnPersist onPersist = executeOnPersist(update.onPersist(), durableDao, durable);
          Iterable<Durable> finderIterable = executeFinder(update.finder(), durableDao, durable);

          for (Durable indexingDurable : finderIterable) {

            Operand operand = executeProxy(update.proxy(), durableDao, indexingDurable);

            switch (onPersist) {
              case INSERT:
                vectoredDao.updateInVector(new VectorKey(VectorIndices.getVectorIndexes(update.value(), operand.getDurable()), durableDao.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), indexingDurable);
                break;
              case REMOVE:
                vectoredDao.removeFromVector(new VectorKey(VectorIndices.getVectorIndexes(update.value(), operand.getDurable()), durableDao.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), indexingDurable);
                break;
              default:
                throw new UnknownSwitchCaseException(onPersist.name());
            }
          }
        }
      }

      for (Invalidate invalidate : cachedWith.invalidates()) {
        if (executeFilter(invalidate.filter(), durableDao, durable)) {

          Iterable<Durable> finderIterable = executeFinder(invalidate.finder(), durableDao, durable);

          for (Durable indexingDurable : finderIterable) {

            Operand operand = executeProxy(invalidate.proxy(), durableDao, indexingDurable);

            vectoredDao.deleteVector(new VectorKey(VectorIndices.getVectorIndexes(invalidate.value(), operand.getDurable()), durableDao.getManagedClass(), Classifications.get(CachedWith.class, null, invalidate.value())));
          }
        }
      }

      return durable;
    }
  }

  @Around(value = "execution(void delete (..)) && @within(CachedWith) && args(durable) && this(durableDao)", argNames = "thisJoinPoint, durableDao, durable")
  public void aroundDeleteMethod (ProceedingJoinPoint thisJoinPoint, VectorAwareDurableDao durableDao, Durable durable)
    throws Throwable {

    CachedWith cachedWith;
    VectoredDao vectoredDao;

    if (((vectoredDao = durableDao.getVectoredDao()) == null) || ((cachedWith = durableDao.getClass().getAnnotation(CachedWith.class)) == null)) {

      thisJoinPoint.proceed();
    }
    else {
      thisJoinPoint.proceed();

      for (Update update : cachedWith.updates()) {
        if (executeFilter(update.filter(), durableDao, durable)) {

          Iterable<Durable> finderIterable = executeFinder(update.finder(), durableDao, durable);

          for (Durable indexingDurable : finderIterable) {

            Operand operand = executeProxy(update.proxy(), durableDao, indexingDurable);

            vectoredDao.removeFromVector(new VectorKey(VectorIndices.getVectorIndexes(update.value(), operand.getDurable()), durableDao.getManagedClass(), Classifications.get(CachedWith.class, null, update.value())), indexingDurable);
          }
        }
      }

      for (Invalidate invalidate : cachedWith.invalidates()) {
        if (executeFilter(invalidate.filter(), durableDao, durable)) {

          Iterable<Durable> finderIterable = executeFinder(invalidate.finder(), durableDao, durable);

          for (Durable indexingDurable : finderIterable) {

            Operand operand = executeProxy(invalidate.proxy(), durableDao, indexingDurable);

            vectoredDao.deleteVector(new VectorKey(VectorIndices.getVectorIndexes(invalidate.value(), operand.getDurable()), durableDao.getManagedClass(), Classifications.get(CachedWith.class, null, invalidate.value())));
          }
        }
      }
    }
  }

  private boolean executeFilter (String filterMethodName, VectorAwareDurableDao durableDao, Durable durable) {

    Method filterMethod;

    if (filterMethodName.length() > 0) {

      if ((filterMethod = locateMethod(durableDao, filterMethodName, durableDao.getManagedClass())) == null) {
        throw new CacheAutomationError("The filter Method(%s) referenced within @CachedWith does not exist", filterMethodName);
      }

      if (!(filterMethod.getReturnType().equals(boolean.class) || filterMethod.getReturnType().equals(Boolean.class))) {
        throw new CacheAutomationError("A filter Method(%s) referenced by @CachedWith must return a value of type 'boolean'", filterMethodName);
      }

      try {

        return (Boolean)filterMethod.invoke(durableDao, durable);
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return true;
  }

  private OnPersist executeOnPersist (String onPersistMethodName, VectorAwareDurableDao durableDao, Durable durable) {

    Method onPersistMethod;

    if (onPersistMethodName.length() > 0) {

      if ((onPersistMethod = locateMethod(durableDao, onPersistMethodName, durableDao.getManagedClass())) == null) {
        throw new CacheAutomationError("The onPersist Method(%s) referenced within @CachedWith does not exist", onPersistMethodName);
      }

      if (!onPersistMethod.getReturnType().equals(OnPersist.class)) {
        throw new CacheAutomationError("An onPersist Method(%s) referenced by @CachedWith must return a value of type 'OnPersist'", onPersistMethodName);
      }

      try {

        return (OnPersist)onPersistMethod.invoke(durableDao, durable);
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }

    return OnPersist.INSERT;
  }

  private Operand executeProxy (Proxy proxy, VectorAwareDurableDao durableDao, Durable durable) {

    Method proxyMethod;
    Class<? extends Durable> expectedType;

    if ((proxy.method() == null) || (proxy.method().length() == 0)) {

      return new Operand(durableDao.getManagedClass(), durable);
    }

    if ((proxyMethod = locateMethod(durableDao, proxy.method(), durableDao.getManagedClass())) == null) {
      throw new CacheAutomationError("The proxy method(%s) does not exist", proxy.method());
    }
    if (!(expectedType = proxy.with().equals(Durable.class) ? durable.getClass() : proxy.with()).isAssignableFrom(proxyMethod.getReturnType())) {
      throw new CacheAutomationError("The proxy method(%s) must return a %s type", proxy.method(), expectedType);
    }

    try {

      return new Operand(expectedType, (Durable)proxyMethod.invoke(durableDao, durable));
    }
    catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }

  private Iterable<Durable> executeFinder (Finder finder, VectorAwareDurableDao durableDao, Durable durable) {

    Method finderMethod;
    Type finderReturnType;
    Class<? extends Durable> expectedType;

    if ((finder.method() == null) || (finder.method().length() == 0)) {

      return new SingleItemIterator<Durable>(durable);
    }

    if ((finderMethod = locateMethod(durableDao, finder.method(), durableDao.getManagedClass())) == null) {
      throw new CacheAutomationError("The finder method(%s) does not exist", finder.method());
    }

    if ((expectedType = (finder.with().equals(Durable.class) ? durable.getClass() : finder.with())).isAssignableFrom(finderMethod.getReturnType())) {
      try {

        return new SingleItemIterator<Durable>((Durable)finderMethod.invoke(durableDao, durable));
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }
    else if (!Iterable.class.isAssignableFrom(finderMethod.getReturnType())) {
      if ((!((finderReturnType = finderMethod.getGenericReturnType()) instanceof ParameterizedType)) || (!expectedType.isAssignableFrom((Class)((ParameterizedType)finderReturnType).getActualTypeArguments()[0]))) {
        throw new CacheAutomationError("The finder method(%s) must return an Iterable parameterized to %s <? extends Iterable<? extends %s>>", finder.method(), expectedType.getSimpleName(), expectedType.getSimpleName());
      }

      try {

        return (Iterable<Durable>)finderMethod.invoke(durableDao, durable);
      }
      catch (Exception exception) {
        throw new CacheAutomationError(exception);
      }
    }
    else {
      throw new CacheAutomationError("The finder method(%s) must return either a %s type, or an Iterable parameterized to %s <? extends Iterable<? extends %s>>", finder.method(), expectedType.getSimpleName(), expectedType.getSimpleName(), expectedType.getSimpleName());
    }
  }

  private Method locateMethod (DurableDao durableDao, String methodName, Class parameterType) {

    Method aspectMethod;
    MethodKey methodKey;
    Class[] parameterTypes;

    if ((aspectMethod = METHOD_MAP.get(methodKey = new MethodKey(durableDao.getClass(), methodName))) == null) {
      for (Method method : durableDao.getClass().getMethods()) {
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