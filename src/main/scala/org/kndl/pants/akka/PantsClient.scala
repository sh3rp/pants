package org.kndl.pants.akka

import akka.actor.{Actor, ActorSelection}
import io.netty.channel.ChannelHandlerContext

/**
 * Created by shep on 7/21/2015.
 */

class PantsClient extends Actor {
  val dispatcher: ActorSelection = context.actorSelection("../dispatcher")
  var handlerContext: ChannelHandlerContext = _
  override def receive: Actor.Receive = {
    case OPEN(ctx) =>
      handlerContext = ctx
    case CLOSE(ctx) =>
      handlerContext = _
    case RECV => dispatcher ! _
    case SEND(msg) => handlerContext.writeAndFlush(msg)
  }
}
