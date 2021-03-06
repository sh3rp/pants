package org.kndl.pants

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContext, SslContextBuilder}
import jline.console.ConsoleReader
import org.kndl.pants.netty.client.{ClientHandler, ClientInitializer}

class Client(host: String, port: Int) {

  private val connection: ClientHandler = {
    val ctx: SslContext = SslContextBuilder.forClient()
      .trustManager(InsecureTrustManagerFactory.INSTANCE).build()
    val group: EventLoopGroup = new NioEventLoopGroup()
    val b: Bootstrap = new Bootstrap().group(group).channel(classOf[NioSocketChannel]).handler(new ClientInitializer(ctx))
    val ch: Channel = b.connect(host, port).sync().channel()
    ch.pipeline().get[ClientHandler](classOf[ClientHandler])
  }

  def ping() = connection.sendPing()

  def login(username: String, password: String) = connection.sendLogin(username,password)

  def loggedIn: Boolean = connection.loggedIn

  def join(channel: String) = connection.sendJoinRequest(channel)

  def msg(toUser: String, msg: String) = {
    connection.send
  }
}

object Client extends App {

  val HOST: String = "127.0.0.1"
  val PORT: Int = 8463

  var ctx: SslContext = SslContextBuilder.forClient()
    .trustManager(InsecureTrustManagerFactory.INSTANCE).build()

  val group: EventLoopGroup = new NioEventLoopGroup()
  try {
    val b: Bootstrap = new Bootstrap()
    //b.option(ChannelOption.ALLOCATOR,PooledByteBufAllocator.DEFAULT)
    b.group(group).channel(classOf[NioSocketChannel]).handler(new ClientInitializer(ctx))
    val ch: Channel = b.connect(HOST, PORT).sync().channel()
    val handler: ClientHandler = ch.pipeline().get[ClientHandler](classOf[ClientHandler])
    val con = new ConsoleReader()
    handler.sendLogin("sh3rp","password")
    for (x <- 1 to 10) {
      handler.sendPing()
    }
    Thread.sleep(3000)
    handler.sendJoinRequest("test")
    while(!handler.channelJoined("test")) {
      Thread.sleep(10)
    }
    handler.sendMsg("test","Hello!")
    Thread.sleep(1000)
    handler.sendMsg("test","Good morning!")
    Thread.sleep(5000)
  } finally {
    group.shutdownGracefully()
  }

}




