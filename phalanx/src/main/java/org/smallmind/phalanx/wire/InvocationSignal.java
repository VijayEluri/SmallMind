/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "invocation")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class InvocationSignal implements Signal {

  private Address address;
  private Map<String, Object> arguments;
  private WireContext[] contexts;
  private boolean inOnly;

  public InvocationSignal () {

  }

  public InvocationSignal (boolean inOnly, Address address, Map<String, Object> arguments, WireContext... contexts) {

    this.inOnly = inOnly;
    this.address = address;
    this.arguments = arguments;
    this.contexts = contexts;
  }

  @XmlElement(name = "in_only")
  public boolean isInOnly () {

    return inOnly;
  }

  public void setInOnly (boolean inOnly) {

    this.inOnly = inOnly;
  }

  @XmlElementRef
  public Address getAddress () {

    return address;
  }

  public void setAddress (Address address) {

    this.address = address;
  }

  @XmlJavaTypeAdapter(WireContextXmlAdapter.class)
  @XmlElement(name = "contexts")
  public WireContext[] getContexts () {

    return contexts;
  }

  public void setContexts (WireContext[] contexts) {

    this.contexts = contexts;
  }

  @XmlElement(name = "arguments")
  public Map<String, Object> getArguments () {

    return arguments;
  }

  public void setArguments (Map<String, Object> arguments) {

    this.arguments = arguments;
  }
}
