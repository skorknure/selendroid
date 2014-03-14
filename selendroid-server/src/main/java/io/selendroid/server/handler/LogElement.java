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
package io.selendroid.server.handler;

import io.selendroid.server.RequestHandler;
import io.selendroid.server.Response;
import io.selendroid.server.model.AndroidElement;
import io.selendroid.server.model.AndroidNativeElement;
import io.selendroid.server.model.AndroidWebElement;
import io.selendroid.util.SelendroidLogger;
import io.selendroid.exceptions.SelendroidException;
import io.selendroid.server.SelendroidResponse;
import io.selendroid.server.HttpRequest;

import org.json.JSONException;

public class LogElement extends RequestHandler {

  public LogElement(String mappedUri) {
    super(mappedUri);
  }

  @Override
  public Response handle(HttpRequest request) throws JSONException {
    SelendroidLogger.log("get source of element command");
    String id = getElementId(request);

    AndroidElement element = getElementFromCache(request, id);
    if (element == null) {
      return new SelendroidResponse(getSessionId(request), 10, new SelendroidException("Element with id '" + id
          + "' was not found."));
    }
    if (element instanceof AndroidWebElement) {
      return new SelendroidResponse(getSessionId(request), 12, new SelendroidException(
          "Get source of element is only supported for native elements."));
    }

    return new SelendroidResponse(getSessionId(request), ((AndroidNativeElement) element).toJson().toString());
  }

}
