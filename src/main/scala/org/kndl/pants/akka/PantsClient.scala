package org.kndl.pants.akka

import akka.actor.{Actor, ActorSelection}
import io.netty.channel.ChannelHandlerContext
import org.kndl.pants.PantsCapable
import org.kndl.pants.PantsProtocol.Pants
import org.kndl.pants.auth.{AUTHORIZE_RESPONSE, AUTHORIZE}
import org.slf4j.{LoggerFactory, Logger}

class PantsClient extends Actor with PantsCapable {

  private val logger: Logger = LoggerFactory.getLogger(classOf[PantsClient])

  val dispatcher: ActorSelection = context.actorSelection("../dispatcher")
  val authenticator: ActorSelection = context.actorSelection("../authenticator")

  var handlerContext: ChannelHandlerContext = _

  override def receive: Actor.Receive = {
    case C_REGISTER(ctx) =>
      handlerContext = ctx
      dispatcher ! D_REGISTER(ctx.hashCode())
    case C_DEREGISTER(ctx) =>
      handlerContext = null
      dispatcher ! D_DEREGISTER(ctx.hashCode())
    case in: IN =>
      in.msg.getType match {
        case Pants.Type.LOGIN_REQUEST =>
          authenticator ! AUTHORIZE(in.msg.getUsername(),in.msg.getPassword)
        case _ =>
          dispatcher ! in
      }
    case authorized: AUTHORIZE_RESPONSE =>
      handlerContext.writeAndFlush(newLoginResponse(authorized.user.id,authorized.user.authorized))
    case out: OUT =>
      handlerContext.writeAndFlush(out.msg)
    case _ => // discard
  }
}
