package org.smallmind.nutsnbolts.swing.event;

import java.util.EventListener;

public interface MemoryUsageListener extends EventListener {

   public abstract void usageUpdate (MemoryUsageEvent memeoryUsageEvent);

}
