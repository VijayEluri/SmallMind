/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.persistence.orm.type;

import java.io.Serializable;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class Money implements Serializable {

  private static final Map<Character, Locale> CURRENCY_MAP = new HashMap<Character, Locale>();

  private Currency currency;
  private double amount;

  static {

    CURRENCY_MAP.put('$', Locale.US);
  }

  public static Locale getLocaleForCurrencySymbol (char symbol) {

    return CURRENCY_MAP.get(symbol);
  }

  public static Money createEmptyInstance (Locale locale) {

    return new Money(Currency.getInstance(locale), 0);
  }

  public Money () {

  }

  public Money (Currency currency, double amount) {

    this.currency = currency;
    this.amount = amount;
  }

  @XmlJavaTypeAdapter(CurrencyAdapter.class)
  public Currency getCurrency () {

    return currency;
  }

  public void setCurrency (Currency currency) {

    this.currency = currency;
  }

  public double getAmount () {

    return amount;
  }

  public void setAmount (double amount) {

    this.amount = amount;
  }
}
