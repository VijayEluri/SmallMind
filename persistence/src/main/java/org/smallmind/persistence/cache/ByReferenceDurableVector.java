package org.smallmind.persistence.cache;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorPredicate;
import org.smallmind.persistence.cache.util.CachedList;
import org.terracotta.modules.annotations.AutolockRead;
import org.terracotta.modules.annotations.AutolockWrite;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class ByReferenceDurableVector<I extends Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

   private CachedList<D> elements;

   public ByReferenceDurableVector (CachedList<D> elements, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

      super(comparator, maxSize, timeToLive, ordered);

      this.elements = elements;
      if (maxSize > 0) {
         while (elements.size() > maxSize) {
            elements.removeLast();
         }
      }
   }

   @AutolockRead
   public DurableVector<I, D> copy () {

      return new ByReferenceDurableVector<I, D>(new CachedList<D>(elements), getComparator(), getMaxSize(), getTimeToLive(), isOrdered());
   }

   public boolean isSingular () {

      return false;
   }

   @AutolockWrite
   public synchronized void add (D durable) {

      if (durable != null) {

         if (isOrdered()) {

            Iterator<D> elementIter = elements.iterator();
            D element;
            boolean removed = false;
            boolean zoned = false;
            boolean inserted = false;
            int index = 0;

            while ((!(removed && zoned)) && elementIter.hasNext()) {
               element = elementIter.next();

               if (element.equals(durable)) {
                  if (((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) == 0) {
                     zoned = true;
                     inserted = true;
                  }
                  else {
                     elementIter.remove();
                  }

                  removed = true;
               }
               else if (((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) >= 0) {
                  zoned = true;
               }
               else if (!zoned) {
                  index++;
               }
            }

            if (!inserted) {
               elements.add(index, durable);
            }
         }
         else {

            boolean matched = false;

            for (D element : elements) {
               if (element.equals(durable)) {
                  matched = true;
                  break;
               }
            }

            if (!matched) {
               elements.addFirst(durable);
            }
         }

         if ((getMaxSize() > 0) && (elements.size() > getMaxSize())) {
            elements.removeLast();
         }
      }
   }

   @AutolockWrite
   public synchronized void remove (D durable) {

      boolean removed;

      do {
         removed = elements.remove(durable);
      } while (removed);
   }

   @AutolockWrite
   public void removeId (I id) {

      Iterator<D> elementIter = elements.iterator();

      while (elementIter.hasNext()) {
         if (elementIter.next().getId().equals(id)) {
            elementIter.remove();
         }
      }
   }

   @AutolockWrite
   public void filter (VectorPredicate<D> predicate) {

      Iterator<D> elementIter = elements.iterator();

      while (elementIter.hasNext()) {
         if (!predicate.isValid(elementIter.next())) {
            elementIter.remove();
         }
      }
   }

   @AutolockRead
   public synchronized D head () {

      if (elements.isEmpty()) {
         return null;
      }

      return elements.get(0);
   }

   @AutolockRead
   public synchronized List<D> asList () {

      return Collections.unmodifiableList(elements);
   }

   @AutolockRead
   public synchronized Iterator<D> iterator () {

      return Collections.unmodifiableList(elements).iterator();
   }
}
