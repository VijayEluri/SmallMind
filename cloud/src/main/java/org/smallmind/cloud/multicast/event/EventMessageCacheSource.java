/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.cloud.multicast.event;

import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.indigenous.CacheReference;
import org.smallmind.quorum.cache.indigenous.CacheSource;

public class EventMessageCacheSource implements CacheSource<EventMessageKey, EventMessageMold, EventMessageCacheEntry> {

   public EventMessageCacheEntry createEntry (EventMessageKey key, Object... parameters)
      throws CacheException {

      return new EventMessageCacheEntry(new EventMessageMold());
   }

   public CacheReference<EventMessageKey, EventMessageCacheEntry> wrapReference (EventMessageKey key, EventMessageMold value)
      throws CacheException {

      return new CacheReference<EventMessageKey, EventMessageCacheEntry>(key, new EventMessageCacheEntry(new EventMessageMold()));
   }
}
