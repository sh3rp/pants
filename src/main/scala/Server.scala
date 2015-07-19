import java.io.FileReader
import java.util.Properties

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
  val connections: Map[Int,Channel] = Map()
  val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
  val workerGroup: EventLoopGroup = new NioEventLoopGroup()
  try {
    val b: ServerBootstrap = new ServerBootstrap()
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new ServerInitializer(sslCtx,connections))

    b.bind(PORT).sync().channel().closeFuture().sync()
  } finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}

class ServerInitializer(ctx: SslContext, connections: Map[Int,Channel]) extends ChannelInitializer[SocketChannel] {
  override def initChannel(c: SocketChannel): Unit = {
    val pipeline: ChannelPipeline = c.pipeline()
    pipeline.addLast(ctx.newHandler(c.alloc()))

    // inbound

    pipeline.addLast(new ProtobufVarint32FrameDecoder())
    pipeline.addLast(new ProtobufDecoder(PantsProtocol.Pants.getDefaultInstance()))

    // outbound

    pipeline.addLast(new ProtobufVarint32LengthFieldPrepender())
    pipeline.addLast(new ProtobufEncoder())

    // object

    pipeline.addLast(new PantsHandler(connections))

  }
}

class PantsHandler(connections: Map[Int,Channel]) extends SimpleChannelInboundHandler[PantsProtocol.Pants] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[PantsHandler])

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    connections.put(ctx.channel.hashCode(),ctx.channel)
  }

  override def handlerRemoved(ctx: ChannelHandlerContext) {
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: Pants): Unit = {
    try {
      handle(ctx, msg.getType, msg.getData)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, c: Throwable): Unit = {
    LOGGER.error(c.getMessage)
  }

  def handle(ctx: ChannelHandlerContext, pType: PantsProtocol.Pants.Type, data: ByteString) = {
    pType match {
      case PantsProtocol.Pants.Type.PING =>
        LOGGER.info("PING <- {}",ctx.name())
        ctx.writeAndFlush(newPong)
        LOGGER.info("PONG -> {}",ctx.name())
      case PantsProtocol.Pants.Type.MSG =>
        val inMsg: PantsProtocol.Msg = PantsProtocol.Msg.parseFrom(data)
        LOGGER.info("Got message: {}",inMsg.getMessage)
        LOGGER.info("Sending to: {}",connections)
        connections.foreach { conn =>
          conn._2.writeAndFlush(newMsg(inMsg.getMessage))
        }
    }
  }

  def newMsg(msg: String): PantsProtocol.Pants = {
    PantsProtocol.Pants.newBuilder()
      .setType(PantsProtocol.Pants.Type.MSG)
      .setData(PantsProtocol.Msg.newBuilder()
        .setMessage(msg).build().toByteString)
      .build()
  }

  def newPong: PantsProtocol.Pants = {
    PantsProtocol.Pants.newBuilder()
      .setType(PantsProtocol.Pants.Type.PONG)
      .setData(
        PantsProtocol.Pong.newBuilder()
          .setVersion(version)
          .build()
          .toByteString
      ).build()
  }

  def version: PantsProtocol.Version = {
    PantsProtocol.Version.newBuilder()
      .setMajor(Server.vma)
      .setMinor(Server.vmi)
      .setPatch(Server.vpa)
      .build()
  }
}


