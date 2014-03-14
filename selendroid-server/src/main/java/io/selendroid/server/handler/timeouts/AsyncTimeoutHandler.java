package io.selendroid.server.handler.timeouts;

import io.selendroid.server.RequestHandler;
import io.selendroid.server.Response;
import io.selendroid.server.SelendroidResponse;
import io.selendroid.server.HttpRequest;

import org.json.JSONException;

public class AsyncTimeoutHandler extends RequestHandler {
  public AsyncTimeoutHandler(String mappedUri) {
    super(mappedUri);
  }
  @Override
  public Response handle(HttpRequest request) throws JSONException {
    getSelendroidDriver(request).setAsyncTimeout(getPayload(request).getLong("ms"));
    return new SelendroidResponse(getSessionId(request), null);
  }
}
