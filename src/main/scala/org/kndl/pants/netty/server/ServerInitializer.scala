package org.kndl.pants.netty.server

import akka.actor.{ActorSystem, Props}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, ChannelPipeline}
import io.netty.handler.codec.protobuf.{ProtobufDecoder, ProtobufEncoder, ProtobufVarint32FrameDecoder, ProtobufVarint32LengthFieldPrepender}
import io.netty.handler.ssl.SslContext
import org.kndl.pants.PantsProtocol
import org.kndl.pants.akka.PantsClient

/**
 *
 *
 * @author shep
 *
 **/
class ServerInitializer(ctx: SslContext, system: ActorSystem) extends ChannelInitializer[SocketChannel] {
  override def initChannel(c: SocketChannel): Unit = {
    val pipeline: ChannelPipeline = c.pipeline()
    pipeline.addLast(ctx.newHandler(c.alloc()))

    // inbound

    pipeline.addLast(new ProtobufVarint32FrameDecoder())
    pipeline.addLast(new ProtobufDecoder(PantsProtocol.Pants.getDefaultInstance()))

    // outbound

    pipeline.addLast(new ProtobufVarint32LengthFieldPrepender())
    pipeline.addLast(new ProtobufEncoder())

    // object

    val client = system.actorOf(Props[PantsClient])
    val handler: ServerHandler = new ServerHandler(client)
    pipeline.addLast(handler)

  }
}