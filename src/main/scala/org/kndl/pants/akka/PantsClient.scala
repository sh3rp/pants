package org.kndl.pants.akka

import akka.actor.{Actor, ActorSelection}
import io.netty.channel.ChannelHandlerContext
import org.slf4j.{LoggerFactory, Logger}

class PantsClient extends Actor {

  private val logger: Logger = LoggerFactory.getLogger(classOf[PantsClient])

  val dispatcher: ActorSelection = context.actorSelection("../dispatcher")
  var handlerContext: ChannelHandlerContext = _

  override def receive: Actor.Receive = {
    case C_REGISTER(ctx) =>
      handlerContext = ctx
      dispatcher ! D_REGISTER(ctx.hashCode())
    case C_DEREGISTER(ctx) =>
      handlerContext = null
      dispatcher ! D_DEREGISTER(ctx.hashCode())
    case in: IN =>
      dispatcher ! in
    case out: OUT =>
      handlerContext.writeAndFlush(out.msg)
    case _ => // discard
  }
}
