package org.kndl.pants.akka

import io.netty.channel.ChannelHandlerContext
import org.kndl.pants.PantsProtocol.Pants

case class C_REGISTER(ctx: ChannelHandlerContext)
case class C_DEREGISTER(ctx: ChannelHandlerContext)
case class D_REGISTER(ctxId: Int)
case class D_DEREGISTER(ctxId: Int)
case class IN(msg: Pants)
case class OUT(msg: Pants)

