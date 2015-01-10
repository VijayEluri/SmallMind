/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.oauth.v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sso")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class SSOAuthData {

  private String user;
  private String password;
  private long created;

  public SSOAuthData () {

  }

  public SSOAuthData (String user, String password) {

    this.user = user;
    this.password = password;

    created = System.currentTimeMillis();
  }

  @XmlElement(name = "user", required = true, nillable = false)
  public String getUser () {

    return user;
  }

  public void setUser (String user) {

    this.user = user;
  }

  @XmlElement(name = "password", required = true, nillable = false)
  public String getPassword () {

    return password;
  }

  public void setPassword (String password) {

    this.password = password;
  }

  @XmlElement(name = "created", required = true, nillable = false)
  public long getCreated () {

    return created;
  }

  public void setCreated (long created) {

    this.created = created;
  }
}