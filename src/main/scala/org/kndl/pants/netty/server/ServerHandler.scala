package org.kndl.pants.netty.server

import akka.actor.ActorRef
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.Pants
import org.slf4j.{Logger, LoggerFactory}

class ServerHandler(client: ActorRef) extends SimpleChannelInboundHandler[PantsProtocol.Pants] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[ServerHandler])

  var channel: Channel = _

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    client ! OPEN(ctx)
  }

  override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
    client ! CLOSE(ctx)
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: Pants): Unit = {
    try {
      client ! RECV(ctx.hashCode(),msg)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, c: Throwable): Unit = {
    LOGGER.error(c.getMessage)
  }
}
