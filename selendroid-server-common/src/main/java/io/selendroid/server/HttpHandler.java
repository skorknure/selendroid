package io.selendroid.server;


public interface HttpHandler {

  void handleHttpRequest(HttpRequest request, HttpResponse response) throws Exception;

}
