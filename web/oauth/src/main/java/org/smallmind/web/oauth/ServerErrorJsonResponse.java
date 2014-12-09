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
package org.smallmind.web.oauth;

public class ServerErrorJsonResponse {

  private String error = "unknown";
  private String errorDescription;
  private String errorUri;

  private ServerErrorJsonResponse () {

  }

  public static ServerErrorJsonResponse instance () {

    return new ServerErrorJsonResponse();
  }

  public ServerErrorJsonResponse setError (String error) {

    if ((error != null) && (!error.isEmpty())) {
      this.error = error;
    }

    return this;
  }

  public ServerErrorJsonResponse setErrorDescription (String errorDescription) {

    this.errorDescription = errorDescription;

    return this;
  }

  public ServerErrorJsonResponse setErrorUri (String errorUri) {

    this.errorUri = errorUri;

    return this;
  }

  public String build () {

    StringBuilder jsonBuilder = new StringBuilder("{\"error\": \"").append(error).append('"');

    if ((errorDescription != null) && (!errorDescription.isEmpty())) {
      jsonBuilder.append(", \"error_description\": \"").append(errorDescription).append('"');
    }
    if ((errorUri != null) && (!errorUri.isEmpty())) {
      jsonBuilder.append(", \"error_uri\": \"").append(errorUri).append('"');
    }

    return jsonBuilder.append('}').toString();
  }
}