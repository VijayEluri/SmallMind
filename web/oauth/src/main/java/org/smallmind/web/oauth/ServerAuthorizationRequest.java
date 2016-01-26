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
package org.smallmind.web.oauth;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class ServerAuthorizationRequest {

  private String clientId;
  private String responseType;
  private String loginUri;
  private String redirectUri;
  private String state;
  private String scope;
  private String authData;

  public ServerAuthorizationRequest (HttpServletRequest request)
    throws OAuthProtocolException {

    Map<String, String[]> paramMap = request.getParameterMap();

    clientId = getValue(paramMap, "client id", "client_id", true);
    responseType = getValue(paramMap, "response type", "response_type", true);
    loginUri = getValue(paramMap, "login uri", "login_uri", false);
    redirectUri = getValue(paramMap, "redirect uri", "redirect_uri", false);
    state = getValue(paramMap, "state", "state", false);
    scope = getValue(paramMap, "scope", "scope", false);
    authData = getValue(paramMap, "auth data", "auth_data", false);
  }

  private String getValue (Map<String, String[]> paramMap, String name, String key, boolean required)
    throws OAuthProtocolException {

    String[] values;

    if ((values = paramMap.get(key)) != null) {
      if (values.length > 1) {
        throw new MultipleParameterException("multiple %s", name);
      }

      return values[0];
    }

    if (required) {
      throw new MissingParameterException("missing %s", name);
    }

    return null;
  }

  public String getClientId () {

    return clientId;
  }

  public String getResponseType () {

    return responseType;
  }

  public String getLoginUri () {

    return loginUri;
  }

  public String getRedirectUri () {

    return redirectUri;
  }

  public String getState () {

    return state;
  }

  public String getScope () {

    return scope;
  }

  public String getAuthData () {

    return authData;
  }
}