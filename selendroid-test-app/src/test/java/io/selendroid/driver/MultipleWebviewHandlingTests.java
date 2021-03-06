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
package io.selendroid.driver;

import static io.selendroid.waiter.TestWaiter.waitFor;

import io.netty.handler.codec.http.HttpMethod;
import io.selendroid.SelendroidDriver;
import io.selendroid.server.util.HttpClientUtil;
import io.selendroid.support.BaseAndroidTest;
import io.selendroid.waiter.WaitingConditions;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;


public class MultipleWebviewHandlingTests extends BaseAndroidTest {
  @Test
  public void testShouldBeAbleToReadWindowTitleOfEachWebView() {
    openMultipleWebViewActivity();

    driver().switchTo().window("WEBVIEW_0");
    Assert.assertEquals(driver().getTitle(), "content 1");
    driver().switchTo().window("WEBVIEW_1");
    Assert.assertEquals(driver().getTitle(), "content 2");
  }


  @Test
  public void assertsThatDriverIsAbleToGetWindowHandlesOnWebViewActivity() {
    openMultipleWebViewActivity();
    Set<String> windowHandles = driver().getWindowHandles();
    Assert.assertEquals(windowHandles.size(), 3);
    Assert.assertTrue("Should be able to find native context", windowHandles.contains(NATIVE_APP));
    Assert.assertTrue("Should be able to find webview context 1",
        windowHandles.contains("WEBVIEW_1"));
    Assert.assertTrue("Should be able to find webview context 1",
        windowHandles.contains("WEBVIEW_0"));
  }

  private void openMultipleWebViewActivity() {
    String activityClass = "io.selendroid.testapp." + "MultipleWebViewsActivity";
    driver().switchTo().window(NATIVE_APP);
    driver().get("and-activity://" + activityClass);
    waitFor(WaitingConditions.driverUrlToBe(driver(), "and-activity://MultipleWebViewsActivity"));
  }

  @Test
  public void shouldGetContexts() throws Exception {
    openMultipleWebViewActivity();
    SelendroidDriver driver = driver();
    String uri = "http://localhost:8080/wd/hub/session/" + driver.getSessionId() + "/contexts";

    JSONObject response =
        HttpClientUtil.parseJsonResponse(HttpClientUtil.executeRequest(uri, HttpMethod.GET));
    JSONArray contexts = response.getJSONArray("value");
    Assert.assertEquals(NATIVE_APP, contexts.get(0));
    Assert.assertEquals("WEBVIEW_1", contexts.get(1));
    Assert.assertEquals("WEBVIEW_0", contexts.get(2));
  }

  @Test
  public void shouldGetContext() throws Exception {
    openMultipleWebViewActivity();
    SelendroidDriver driver = driver();
    String uri = "http://localhost:8080/wd/hub/session/" + driver.getSessionId() + "/context";

    JSONObject response =
        HttpClientUtil.parseJsonResponse(HttpClientUtil.executeRequest(uri, HttpMethod.GET));
    Assert.assertEquals(NATIVE_APP, response.getString("value"));
  }

  @Test
  public void shouldSwitchContext() throws Exception {
    openMultipleWebViewActivity();
    SelendroidDriver driver = driver();
    String uri = "/wd/hub/session/" + driver.getSessionId() + "/context";

    HttpClientUtil.parseJsonResponse(HttpClientUtil.executeRequestWithPayload(uri, 8080,
        HttpMethod.POST, "{'name':'WEBVIEW_0'}"));
    String getContextUri =
        "http://localhost:8080/wd/hub/session/" + driver.getSessionId() + "/context";

    JSONObject response =
        HttpClientUtil.parseJsonResponse(HttpClientUtil.executeRequest(getContextUri,
            HttpMethod.GET));
    Assert.assertEquals("WEBVIEW_0", response.getString("value"));
  }
}
