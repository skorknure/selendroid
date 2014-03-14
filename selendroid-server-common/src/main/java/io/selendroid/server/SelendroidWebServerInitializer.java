package io.selendroid.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.List;

public class SelendroidWebServerInitializer extends ChannelInitializer<SocketChannel> {

  private final List<HttpHandler> handlers;

  public SelendroidWebServerInitializer(List<HttpHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("codec", new HttpServerCodec());
    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
    pipeline.addLast("handler", new SelendroidWebServerHandler(handlers));
  }

}