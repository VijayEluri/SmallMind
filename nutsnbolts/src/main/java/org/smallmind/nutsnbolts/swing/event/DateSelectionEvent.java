package org.smallmind.nutsnbolts.swing.event;

import java.util.EventObject;
import org.smallmind.nutsnbolts.swing.calendar.CalendarDate;

public class DateSelectionEvent extends EventObject {

   private CalendarDate calendarDate;

   public DateSelectionEvent (Object source, CalendarDate calendarDate) {

      super(source);

      this.calendarDate = calendarDate;
   }

   public CalendarDate getCalendarDate () {

      return calendarDate;
   }

}
