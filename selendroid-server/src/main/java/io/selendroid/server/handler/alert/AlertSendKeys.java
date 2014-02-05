/*
 * Copyright 2014 eBay Software Foundation and selendroid committers.
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
package io.selendroid.server.handler.alert;

import io.selendroid.exceptions.SelendroidException;
import io.selendroid.server.RequestHandler;
import io.selendroid.server.Response;
import io.selendroid.server.SelendroidResponse;
import org.json.JSONException;
import org.webbitserver.HttpRequest;

public class AlertSendKeys extends RequestHandler {

  public AlertSendKeys(String mappedUri) {
    super(mappedUri);
  }

  @Override
  public Response handle(HttpRequest request) throws JSONException {
    if (!getSelendroidDriver(request).isAlertPresent()) {
      return new SelendroidResponse(getSessionId(request), 27, "no alert open");
    }
    String keysToSend = null;
    try {
      keysToSend = getPayload(request).getString("text");
    } catch (SelendroidException e) {
      return new SelendroidResponse(getSessionId(request), 13, e);
    }
    getSelendroidDriver(request).setAlertText(keysToSend);
    return new SelendroidResponse(getSessionId(request), null);
  }

  @Override
  public boolean commandAllowedWithAlertPresentInWebViewMode() {
    return true;
  }
}
