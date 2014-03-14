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
package io.selendroid.server.handler;

import io.selendroid.server.RequestHandler;
import io.selendroid.server.Response;
import io.selendroid.server.SelendroidResponse;
import io.selendroid.server.model.Session;
import io.selendroid.util.SelendroidLogger;
import io.selendroid.server.HttpRequest;

import org.json.JSONException;

/**
 * Determine the configuration of a command.
 */
public class GetCommandConfiguration extends RequestHandler {

  public GetCommandConfiguration(String mappedUri) {
    super(mappedUri);
  }

  @Override
  public Response handle(HttpRequest request) throws JSONException {
    SelendroidLogger.log("Get command configuration");
    Session session = getSelendroidDriver(request).getSession();
    String command = getCommandName(request);
    return new SelendroidResponse(getSessionId(request), session.getCommandConfiguration(command));
  }
}
