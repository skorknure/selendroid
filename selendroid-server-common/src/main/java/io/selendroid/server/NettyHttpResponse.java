package io.selendroid.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

public class NettyHttpResponse implements HttpResponse {

  private final FullHttpResponse response;
  private Charset charset = CharsetUtil.UTF_8;
  private boolean closed;

  public NettyHttpResponse(FullHttpResponse response) {
    this.response = response;
  }

  @Override
  public void setStatus(int status) {
    response.setStatus(HttpResponseStatus.valueOf(status));
  }

  @Override
  public void setContentType(String mimeType) {
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeType);
  }

  @Override
  public void setContentEncoding(String encoding) {
    response.headers().set(HttpHeaders.Names.CONTENT_ENCODING, encoding);
  }

  @Override
  public void setLocation(String url) {
    response.headers().set(HttpHeaders.Names.LOCATION, url);
  }

  @Override
  public void setContentLength(int length) {
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, length);
  }

  @Override
  public void setContent(byte[] data) {
    response.content().setBytes(0, data);
    // TODO: >>>! remove ???
    setContentLength(response.content().readableBytes());
  }

  @Override
  public void setContent(String message) {
    ByteBuf buffer = Unpooled.copiedBuffer(message, charset);
    response.content().writeBytes(buffer);
    // TODO: >>>! remove ???
    // TODO: >>>! remove ???
    setContentLength(response.content().readableBytes());
    buffer.release();
  }

  @Override
  public void setEncoding(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void sendRedirect(String to) {
    setStatus(301);
    setLocation(to);
    end();
  }

  @Override
  public void sendTemporaryRedirect(String to) {
    setStatus(302);
    setLocation(to);
    end();
  }

  @Override
  public void end() {
    closed = true;
  }

  public boolean isClosed() {
    return closed;
  }

}
