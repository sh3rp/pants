package org.kndl.pants.netty.server

import akka.actor.ActorRef
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import org.kndl.pants.{Server, PantsProtocol}
import org.kndl.pants.PantsProtocol.Pants
import org.kndl.pants.akka._
import org.slf4j.{Logger, LoggerFactory}

class ServerHandler(client: ActorRef) extends SimpleChannelInboundHandler[PantsProtocol.Pants] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[ServerHandler])

  var channel: Channel = _

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    client ! C_REGISTER(ctx)
  }

  override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
    client ! C_DEREGISTER(ctx)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: Pants): Unit = {
    try {
      msg.getType match {
        case PantsProtocol.Pants.Type.PING =>
          ctx.writeAndFlush(newPong(System.currentTimeMillis()))
        case _ => client ! IN(msg)
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, c: Throwable): Unit = {
    LOGGER.error(c.getMessage)
  }
  def newPong(timestamp: Long): PantsProtocol.Pants = {
    PantsProtocol.Pants.newBuilder()
      .setType(PantsProtocol.Pants.Type.PONG)
      .setData(
        PantsProtocol.Pong.newBuilder()
          .setVersion(version)
          .setTimestamp(timestamp)
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
