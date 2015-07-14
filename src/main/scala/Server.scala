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
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.{Ping, Pants}
import org.slf4j.{Logger, LoggerFactory}

object Server extends App {

  val LOGGER: Logger = LoggerFactory.getLogger("Server")

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
    val b: ServerBootstrap = new ServerBootstrap()
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new ServerInitializer(sslCtx))

    b.bind(PORT).sync().channel().closeFuture().sync()
  } finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}

class ServerInitializer(ctx: SslContext) extends ChannelInitializer[SocketChannel] {
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

    pipeline.addLast(new PantsHandler())

  }
}

class PantsHandler extends SimpleChannelInboundHandler[PantsProtocol.Pants] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[PantsHandler])

  override def channelRead0(ctx: ChannelHandlerContext, msg: Pants): Unit = {
    LOGGER.debug("Reading from channel")
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
        LOGGER.info("Received PING, sending PONG")
        ctx.writeAndFlush(newPong)
    }
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
    PantsProtocol.Version.newBuilder().setMajor(1).setMinor(0).setPatch(0).build()
  }
}


