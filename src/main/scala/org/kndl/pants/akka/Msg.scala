package org.kndl.pants.akka

import io.netty.channel.ChannelHandlerContext
import org.kndl.pants.PantsProtocol.Pants

case class RECV(ctxId: Int, packet: Pants)
case class SEND(packet: Pants)
case class CLOSE(ctx: ChannelHandlerContext)
case class OPEN(ctx: ChannelHandlerContext)

