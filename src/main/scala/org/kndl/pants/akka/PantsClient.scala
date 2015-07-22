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
      logger.info("Netty Client register: {}",ctx)
      handlerContext = ctx
      dispatcher ! D_REGISTER(ctx.hashCode())
    case C_DEREGISTER(ctx) =>
      logger.info("Netty client deregister: {}",ctx)
      handlerContext = null
      dispatcher ! D_DEREGISTER(ctx.hashCode())
    case in: IN =>
      logger.info("Packet inbound: {}",in.msg)
      dispatcher ! in
    case out: OUT =>
      logger.info("Packet outbound: {}",out.msg)
      handlerContext.writeAndFlush(out.msg)
    case _ => // discard
  }
}
