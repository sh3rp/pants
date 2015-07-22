package org.kndl.pants.netty.client

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, ChannelPipeline}
import io.netty.handler.codec.protobuf.{ProtobufDecoder, ProtobufEncoder, ProtobufVarint32FrameDecoder, ProtobufVarint32LengthFieldPrepender}
import io.netty.handler.ssl.SslContext
import org.kndl.pants.PantsProtocol

/**
 *
 *
 * @author shep
 *
 **/
class ClientInitializer(ctx: SslContext) extends ChannelInitializer[SocketChannel] {
  override def initChannel(c: SocketChannel): Unit = {
    val pipeline: ChannelPipeline = c.pipeline()
    pipeline.addLast(ctx.newHandler(c.alloc(),Client.HOST,Client.PORT))

    // inbound

    pipeline.addLast(new ProtobufVarint32FrameDecoder)
    pipeline.addLast(new ProtobufDecoder(PantsProtocol.Pants.getDefaultInstance))

    // outbound

    pipeline.addLast(new ProtobufVarint32LengthFieldPrepender)
    pipeline.addLast(new ProtobufEncoder())

    pipeline.addLast("client",new ClientHandler)
  }
}