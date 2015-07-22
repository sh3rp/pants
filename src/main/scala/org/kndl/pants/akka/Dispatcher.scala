package org.kndl.pants.akka

import akka.actor.{Actor, ActorRef}
import com.google.protobuf.ByteString
import io.netty.channel.Channel
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.{Pants, Ping}
import org.slf4j.{Logger, LoggerFactory}


class Dispatcher extends Actor {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Dispatcher])

  var connections: Map[Int,Channel] = Map()

  override def receive = {
    case RECV(ctx,packet) =>
      LOGGER.info("Sender: " + sender.path.name + " " + packet.getType.toString + " " + ctx.channel())
      handleMessage(sender(),packet)
  }

  def handleMessage(sender: ActorRef, msg: Pants) = {
    msg.getType match {
      case Pants.Type.PING =>
        val p = ping(msg.getData)
        sender ! RECV()

    }
  }

  def ping(bytes: ByteString): Ping = {
    PantsProtocol.Ping.parseFrom(bytes)
  }

  def newMsg(msg: String): PantsProtocol.Pants = {
    PantsProtocol.Pants.newBuilder()
      .setType(PantsProtocol.Pants.Type.MSG)
      .setData(PantsProtocol.Msg.newBuilder()
      .setMessage(msg).build().toByteString)
      .build()
  }

  def newPong(timestamp: Long): PantsProtocol.Pants = {
    PantsProtocol.Pants.newBuilder()
      .setType(PantsProtocol.Pants.Type.PONG)
      .setData(
        PantsProtocol.Pong.newBuilder()
          .setVersion(version)
          .setTimestamp(timestamp)
          .build()
          .toByteString
      ).build()
  }

  def version: PantsProtocol.Version = {
    PantsProtocol.Version.newBuilder()
      .setMajor(Server.vma)
      .setMinor(Server.vmi)
      .setPatch(Server.vpa)
      .build()
  }
}
