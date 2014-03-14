package io.selendroid.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class SelendroidWebServerHandler extends ChannelInboundHandlerAdapter {

  private List<HttpHandler> httpHandlers;

  public SelendroidWebServerHandler(List<HttpHandler> handlers) {
    this.httpHandlers = handlers;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof FullHttpRequest)) {
      return;
    }

    FullHttpRequest request = (FullHttpRequest) msg;
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);

    NettyHttpRequest httpRequest = new NettyHttpRequest(request);
    NettyHttpResponse httpResponse = new NettyHttpResponse(response);

    for (HttpHandler handler : httpHandlers) {
      handler.handleHttpRequest(httpRequest, httpResponse);
      if (httpResponse.isClosed()) {
        break;
      }
    }

    if (!httpResponse.isClosed()) {
      httpResponse.setStatus(404);
      httpResponse.end();
    }

    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }

}
