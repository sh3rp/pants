package org.kndl.pants.netty.server

import akka.actor.ActorRef
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import org.kndl.pants.{PantsCapable, Server, PantsProtocol}
import org.kndl.pants.PantsProtocol.Pants
import org.kndl.pants.akka._
import org.slf4j.{Logger, LoggerFactory}

class ServerHandler(client: ActorRef) extends SimpleChannelInboundHandler[PantsProtocol.Pants] with PantsCapable {

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

}
