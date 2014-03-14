package io.selendroid.server;

import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NettyHttpRequest implements HttpRequest {

  private final io.netty.handler.codec.http.FullHttpRequest request;
  private Map<String, Object> data;

  public NettyHttpRequest(io.netty.handler.codec.http.FullHttpRequest request) {
    this.request = request;
    this.data = new HashMap<String, Object>();
  }

  @Override
  public String uri() {
    return request.getUri();
  }

  @Override
  public String header(String name) {
    return request.headers().get(name);
  }

  @Override
  public List<String> headers(String name) {
    return request.headers().getAll(name);
  }

  @Override
  public String body() {
    return request.content().toString(CharsetUtil.UTF_8);
  }

  @Override
  public String method() {
    return request.getMethod().toString();
  }

  @Override
  public Map<String, Object> data() {
    return data;
  }

}
