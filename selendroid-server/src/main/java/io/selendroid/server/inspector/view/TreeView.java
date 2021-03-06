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
package io.selendroid.server.inspector.view;

import io.selendroid.ServerInstrumentation;
import io.selendroid.exceptions.SelendroidException;
import io.selendroid.server.inspector.SelendroidInspectorView;
import io.selendroid.server.inspector.TreeUtil;
import io.selendroid.server.model.SelendroidDriver;
import io.selendroid.util.SelendroidLogger;
import io.selendroid.server.HttpRequest;
import io.selendroid.server.HttpResponse;

import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

public class TreeView extends SelendroidInspectorView {
  public TreeView(ServerInstrumentation serverInstrumentation, SelendroidDriver driver) {
    super(serverInstrumentation, driver);
  }

  public void render(HttpRequest request, HttpResponse response) throws JSONException {
    JSONObject source = null;
    try {
      source = driver.getFullWindowTree();
    } catch (SelendroidException e) {
      SelendroidLogger.log("error getting WindowSource in TreeView", e);
      response.setContentType("application/x-javascript");
      response.setEncoding(Charset.forName("UTF-8"));
      response.setContent("{}");
      response.end();
      return;
    }

    JSONObject convertedTree = TreeUtil.createFromNativeWindowsSource(source);
    convertedTree.getJSONObject("metadata").put("xml", TreeUtil.getXMLSource(source));
    response.setContentType("application/x-javascript");
    response.setEncoding(Charset.forName("UTF-8"));
    response.setContent(convertedTree.toString());
    response.end();
  }
}
