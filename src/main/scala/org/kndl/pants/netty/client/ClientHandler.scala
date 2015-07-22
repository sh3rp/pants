package org.kndl.pants.netty.client

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.{Msg, Pants, Ping, Pong}
import org.slf4j.{Logger, LoggerFactory}

/**
 *
 *
 * @author shep
 *
 **/
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
        LOGGER.info("PONG v{}.{}.{} ({}ms)",
          Integer.toString(pong.getVersion().getMajor),
          Integer.toString(pong.getVersion().getMinor),
          Integer.toString(pong.getVersion.getPatch),
          (System.currentTimeMillis() - pong.getTimestamp).toString)
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
      .setData(Ping.newBuilder().setTimestamp(System.currentTimeMillis()).build().toByteString).build()
  }

  def msg(msg: String):Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.MSG)
      .setData(Msg.newBuilder().setMessage(msg).build().toByteString)
      .build()
  }
}