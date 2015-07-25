package org.kndl.pants

import _root_.akka.actor.{ActorSystem, Props}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.handler.ssl.{SslContext, SslContextBuilder}
import org.kndl.pants.akka.Dispatcher
import org.kndl.pants.netty.server.ServerInitializer
import org.slf4j.{Logger, LoggerFactory}

object Server extends App {

  val LOGGER: Logger = LoggerFactory.getLogger("Server")

  val SSL: Boolean = true
  val PORT: Int = 8463
  var sslCtx: SslContext = _
  if (SSL) {
    val ssc: SelfSignedCertificate = new SelfSignedCertificate()
    sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
  } else {
    sslCtx = null;
  }
  val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
  val workerGroup: EventLoopGroup = new NioEventLoopGroup()
  try {

    val actorSystem = ActorSystem("pants")
    val dispatcher = actorSystem.actorOf(Props[Dispatcher], "dispatcher")

    val b: ServerBootstrap = new ServerBootstrap()
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new ServerInitializer(sslCtx, actorSystem))

    b.bind(PORT).sync().channel().closeFuture().sync()
  } finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}






