package org.kndl.pants.akka

import akka.actor.{Actor, ActorRef}
import com.google.protobuf.ByteString
import org.kndl.pants.PantsProtocol.{Pants, Ping}
import org.kndl.pants.{PantsProtocol, Server}
import org.slf4j.{Logger, LoggerFactory}


class Dispatcher extends Actor {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Dispatcher])

  var clients: Map[Int,ActorRef] = Map()

  var names: Map[ActorRef,String] = Map()
  var channels: Map[String,Set[ActorRef]] = Map()

  override def receive = {
    case in: IN =>
      LOGGER.info("Sender: " + sender.path.name + " " + in.msg.getType.toString)
      handleMessage(sender(),in.msg)
  }

  def handleMessage(sender: ActorRef, msg: Pants) = {
    msg.getType match {
      case Pants.Type.MSG =>
      case Pants.Type.PRIVMSG =>
      case Pants.Type.JOIN =>
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
