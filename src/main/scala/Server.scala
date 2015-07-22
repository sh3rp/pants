import java.io.FileReader
import java.util.Properties

import akka.actor._
import akka.actor.Actor.Receive
import com.google.protobuf.ByteString
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.SocketChannel
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufVarint32LengthFieldPrepender, ProtobufDecoder, ProtobufVarint32FrameDecoder}
import io.netty.handler.logging.{LoggingHandler, LogLevel}
import io.netty.handler.ssl.{SslContextBuilder, SslContext}
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.apache.commons.configuration.{PropertiesConfiguration, Configuration}
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.{Ping, Pants}
import org.kndl.pants.akka.Dispatcher
import org.kndl.pants.netty.server.ServerInitializer
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.HashMap
import scala.collection.parallel.mutable
import scala.collection.mutable.Map

object Server extends App {

  val LOGGER: Logger = LoggerFactory.getLogger("Server")

  val vma = 1
  val vmi = 0
  val vpa = 0

  val SSL: Boolean = true
  val PORT: Int = 8463
  var sslCtx: SslContext = _
  if (SSL) {
    val ssc:SelfSignedCertificate = new SelfSignedCertificate()
    sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
  } else {
    sslCtx = null;
  }
  val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
  val workerGroup: EventLoopGroup = new NioEventLoopGroup()
  try {

    val actorSystem = ActorSystem("pants")
    val dispatcher = actorSystem.actorOf(Props[Dispatcher],"dispatcher")

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






