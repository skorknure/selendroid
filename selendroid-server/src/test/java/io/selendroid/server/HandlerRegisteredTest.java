/*
 * Copyright 2012-2013 eBay Software Foundation and selendroid committers.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.selendroid.server;

import io.selendroid.server.internal.SelendroidAssert;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.helpers.NamingThreadFactory;

public class HandlerRegisteredTest extends BaseTest {
  private WebServer server = null;

  @Override
  public void setup() throws Exception {
    //server = WebServers.createWebServer(port);
    URI remoteUri =
        URI.create("http://127.0.0.1"
            + (port == 80 ? "" : (":" + port)) + "/");

    NamingThreadFactory namingThreadFactory =
            new NamingThreadFactory(Executors.defaultThreadFactory(), "selendroid-test-handler");
    server =
        WebServers.createWebServer(Executors.newSingleThreadExecutor(namingThreadFactory),
            new InetSocketAddress(port), remoteUri);
    server.add(new AndroidTestServlet());
    server.start();
  }

  @Test
  public void getFindElementHandlerRegistered() throws Exception {
    JSONObject payload = new JSONObject();
    payload.put("using", "id");
    payload.put("value", "my_button_bar");

    String url = "http://" + host + ":" + port + "/wd/hub/session/1234567890/element";
    HttpResponse response = executeRequestWithPayload(url, HttpMethod.GET, payload.toString());
    SelendroidAssert.assertResponseIsOk(response);
    Assert.assertEquals(
        "{\"status\":0,\"value\":\"sessionId#1234567890 using#id value#my_button_bar\"}",
        IOUtils.toString(response.getEntity().getContent()));
  }

  @Test
  public void postClickHandlerRegistered() throws Exception {
    String url = "http://" + host + ":" + port + "/wd/hub/session/12345/element/815/click";
    HttpResponse response = executeRequest(url, HttpMethod.POST);
    SelendroidAssert.assertResponseIsOk(response);
    Assert.assertEquals("{\"status\":0,\"value\":\"sessionId#12345 elementId#815\"}",
        IOUtils.toString(response.getEntity().getContent()));
  }

  @Test
  public void postStatusHandlerNotRegistered() throws Exception {
    String url = "http://" + host + ":" + port + "/wd/hub/session/1234567890/element";
    HttpResponse response = executeRequest(url, HttpMethod.POST);
    SelendroidAssert.assertResponseIsServerError(response);
  }

  @Override
  public void tearDown() {
    server.stop();
  }
}
