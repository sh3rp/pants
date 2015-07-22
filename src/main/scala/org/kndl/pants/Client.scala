package org.kndl.pants

import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContext, SslContextBuilder}
import jline.console.ConsoleReader
import org.kndl.pants.netty.client.{ClientHandler, ClientInitializer}


object Client extends App {

  val HOST: String = "127.0.0.1"
  val PORT: Int = 8463

  var ctx: SslContext = SslContextBuilder.forClient()
    .trustManager(InsecureTrustManagerFactory.INSTANCE).build()

  val group: EventLoopGroup = new NioEventLoopGroup()
  try {
    val b: Bootstrap = new Bootstrap()
    b.group(group).channel(classOf[NioSocketChannel]).handler(new ClientInitializer(ctx))
    val ch: Channel = b.connect(HOST, PORT).sync().channel()
    val handler: ClientHandler = ch.pipeline().get[ClientHandler](classOf[ClientHandler])
    val con = new ConsoleReader()
    /*    while(true) {
          con.readCharacter() match {
            case 49 =>
              println("sending ping")
              handler.sendPing()
            case 50 =>
              println("sending msg")
              handler.sendMsg("OHAI!")
            case _ =>
          }
        }*/
    for (x <- 1 to 10) {
      handler.sendPing()
    }
    Thread.sleep(5000)
    handler.sendMsg("Hello!")
    Thread.sleep(1000)
    handler.sendMsg("Good morning!")
  } finally {
    group.shutdownGracefully()
  }

}




