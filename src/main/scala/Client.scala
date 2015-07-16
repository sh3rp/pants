import java.io.FileReader
import java.util.Properties

import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.SocketChannel
import io.netty.channel._
import io.netty.channel.nio.{NioEventLoopGroup, NioEventLoop}
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufVarint32LengthFieldPrepender, ProtobufDecoder, ProtobufVarint32FrameDecoder}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContextBuilder, SslContext}
import jline.console.ConsoleReader
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.{Msg, Ping, Pong, Pants}
import org.slf4j.{Logger, LoggerFactory}


object Client extends App {

  val HOST: String = "127.0.0.1"
  val PORT: Int = 8463

  var ctx: SslContext = SslContextBuilder.forClient()
    .trustManager(InsecureTrustManagerFactory.INSTANCE).build()

  val group: EventLoopGroup = new NioEventLoopGroup()
  try {
    val b: Bootstrap = new Bootstrap()
    b.group(group).channel(classOf[NioSocketChannel]).handler(new ClientInitializer(ctx))
    val ch: Channel = b.connect(HOST,PORT).sync().channel()
    val handler: ClientHandler = ch.pipeline().get[ClientHandler](classOf[ClientHandler])
    val con = new ConsoleReader()
    while(true) {
      con.readCharacter() match {
        case 49 => handler.sendPing()
        case 50 => handler.sendMsg("OHAI!")
        case _ =>
      }
    }
    handler.sendPing()
  } finally {
    group.shutdownGracefully()
  }

}

class ClientInitializer(ctx: SslContext) extends ChannelInitializer[SocketChannel] {
  override def initChannel(c: SocketChannel): Unit = {
    val pipeline: ChannelPipeline = c.pipeline()
    pipeline.addLast(ctx.newHandler(c.alloc(),Client.HOST,Client.PORT))

    // inbound

    pipeline.addLast(new ProtobufVarint32FrameDecoder)
    pipeline.addLast(new ProtobufDecoder(PantsProtocol.Pants.getDefaultInstance))

    // outbound

    pipeline.addLast(new ProtobufVarint32LengthFieldPrepender)
    pipeline.addLast(new ProtobufEncoder())

    pipeline.addLast("client",new ClientHandler)

  }
}

class ClientHandler extends SimpleChannelInboundHandler[Pants] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[ClientHandler])

  var ctx: ChannelHandlerContext = _

  def sendPing() = {
    ctx.writeAndFlush(ping)
  }

  def sendMsg(message: String) = {
    ctx.writeAndFlush(msg(message))
  }

  override def channelRegistered(ctx: ChannelHandlerContext) = {
    this.ctx = ctx
  }

  override def channelRead0(channelHandlerContext: ChannelHandlerContext, msg: Pants): Unit = {
    msg.getType() match {
      case PantsProtocol.Pants.Type.PONG =>
        val pong: Pong = PantsProtocol.Pong.parseFrom(msg.getData)
        LOGGER.info("PONG v{}.{}.{}",
          Integer.toString(pong.getVersion().getMajor),
          Integer.toString(pong.getVersion().getMinor),
          Integer.toString(pong.getVersion.getPatch))
      case PantsProtocol.Pants.Type.MSG =>
        val message: Msg = PantsProtocol.Msg.parseFrom(msg.getData)
        LOGGER.info("MSG {}",message.getMessage)
      case _ =>
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, c: Throwable): Unit = {
    c.printStackTrace()
  }

  def ping:Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.PING)
      .setData(Ping.newBuilder().build().toByteString).build()
  }

  def msg(msg: String):Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.MSG)
      .setData(Msg.newBuilder().setMessage(msg).build().toByteString)
      .build()
  }
}
