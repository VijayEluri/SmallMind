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
package org.smallmind.web.jersey.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.WebApplicationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.smallmind.nutsnbolts.http.HttpMethod;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.nutsnbolts.util.Tuple;
import org.smallmind.web.jersey.util.JsonCodec;

public class JsonTarget {

  private final CloseableHttpClient httpClient;
  private final URI uri;
  private Tuple<String, String> headers;
  private Tuple<String, String> queryParameters;

  public JsonTarget (CloseableHttpClient httpClient, URI uri) {

    this.httpClient = httpClient;
    this.uri = uri;
  }

  public JsonTarget path (String path)
    throws URISyntaxException {

    URIBuilder uriBuilder = new URIBuilder().setScheme(uri.getScheme()).setUserInfo(uri.getUserInfo()).setHost(uri.getHost()).setPort(uri.getPort()).setPath(uri.getPath()).setCustomQuery(uri.getQuery()).setFragment(uri.getFragment());
    URI pathURI = URI.create(path);

    if (pathURI.getScheme() != null) {
      uriBuilder.setScheme(pathURI.getScheme());
    }
    if (pathURI.getUserInfo() != null) {
      uriBuilder.setUserInfo(pathURI.getUserInfo());
    }
    if (pathURI.getHost() != null) {
      uriBuilder.setHost(pathURI.getHost());
    }
    if (pathURI.getPort() > 0) {
      uriBuilder.setPort(pathURI.getPort());
    }
    if (pathURI.getPath() != null) {
      uriBuilder.setPath((uri.getPath() == null) ? pathURI.getPath() : uri.getPath() + pathURI.getPath());
    }
    if (pathURI.getRawQuery() != null) {
      uriBuilder.setCustomQuery((uri.getRawQuery() == null) ? pathURI.getQuery() : uri.getQuery() + "&" + pathURI.getQuery());
    }
    if (pathURI.getFragment() != null) {
      uriBuilder.setFragment(pathURI.getFragment());
    }

    return new JsonTarget(httpClient, uriBuilder.build());
  }

  public JsonTarget header (String key, String value) {

    if (headers == null) {
      headers = new Tuple<>();
    }
    headers.addPair(key, value);

    return this;
  }

  public JsonTarget query (String key, String value) {

    if (queryParameters == null) {
      queryParameters = new Tuple<>();
    }
    queryParameters.addPair(key, value);

    return this;
  }

  public <T> T get (Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpGet httpGet = ((HttpGet)createHttpRequest(HttpMethod.GET));

    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T put (HttpEntity entity, Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpPut httpPut = ((HttpPut)createHttpRequest(HttpMethod.PUT));

    httpPut.setEntity(entity);

    try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T post (HttpEntity entity, Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpPost httpPost = ((HttpPost)createHttpRequest(HttpMethod.POST));

    httpPost.setEntity(entity);

    try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T patch (HttpEntity entity, Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpPatch httpPatch = ((HttpPatch)createHttpRequest(HttpMethod.PATCH));

    httpPatch.setEntity(entity);

    try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
      return convertEntity(response, responseClass);
    }
  }

  public <T> T delete (Class<T> responseClass)
    throws IOException, URISyntaxException {

    HttpDelete httpDelete = ((HttpDelete)createHttpRequest(HttpMethod.DELETE));

    try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
      return convertEntity(response, responseClass);
    }
  }

  private <T> T convertEntity (HttpResponse response, Class<T> responseClass)
    throws IOException {

    HttpEntity entity;
    InputStream entityInputStream;

    if (((entity = response.getEntity()) == null) || (entity.getContentLength() == 0) || ((entityInputStream = entity.getContent()) == null)) {

      StatusLine statusLine;

      if (((statusLine = response.getStatusLine()) == null) || ((statusLine.getStatusCode() >= 200) && statusLine.getStatusCode() < 300)) {

        return null;
      }

      throw new WebApplicationException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
    }

    return JsonCodec.convert(entityInputStream, responseClass);
  }

  private HttpRequest createHttpRequest (HttpMethod httpMethod)
    throws URISyntaxException {

    HttpRequest httpRequest;

    if (queryParameters == null) {
      switch (httpMethod) {
        case GET:
          httpRequest = new HttpGet(uri);
          break;
        case PUT:
          httpRequest = new HttpPut(uri);
          break;
        case POST:
          httpRequest = new HttpPost(uri);
          break;
        case PATCH:
          httpRequest = new HttpPatch(uri);
          break;
        case DELETE:
          httpRequest = new HttpDelete(uri);
          break;
        default:
          throw new UnknownSwitchCaseException(httpMethod.name());
      }
    } else {

      URIBuilder uriBuilder = new URIBuilder(uri);

      for (Pair<String, String> queryPair : queryParameters) {
        uriBuilder.addParameter(queryPair.getFirst(), queryPair.getSecond());
      }

      switch (httpMethod) {
        case GET:
          httpRequest = new HttpGet(uriBuilder.build());
          break;
        case PUT:
          httpRequest = new HttpPut(uriBuilder.build());
          break;
        case POST:
          httpRequest = new HttpPost(uriBuilder.build());
          break;
        case PATCH:
          httpRequest = new HttpPatch(uriBuilder.build());
          break;
        case DELETE:
          httpRequest = new HttpDelete(uriBuilder.build());
          break;
        default:
          throw new UnknownSwitchCaseException(httpMethod.name());
      }
    }

    if (headers != null) {
      for (Pair<String, String> headerPair : headers) {
        httpRequest.setHeader(headerPair.getFirst(), headerPair.getSecond());
      }
    }

    httpRequest.setHeader("Content-type", "application/json");

    return httpRequest;
  }
}
