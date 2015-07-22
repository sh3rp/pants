package org.kndl.pants.akka

import akka.actor.{Actor, ActorRef}
import com.google.protobuf.ByteString
import org.kndl.pants.PantsProtocol.{Pants, Ping}
import org.kndl.pants.{PantsProtocol, Server}
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.immutable.Map


class Dispatcher extends Actor {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Dispatcher])

  var clients: Map[Int,ActorRef] = Map()

  var names: Map[ActorRef,String] = Map()
  var channels: Map[Int,Set[ActorRef]] = Map()

  override def receive = {
    case in: IN =>
      LOGGER.info("Sender: " + sender.path.name + " " + in.msg.getType.toString)
      handleMessage(sender(),in.msg)
  }

  def handleMessage(sender: ActorRef, msg: Pants) = {
    msg.getType match {
      case Pants.Type.MSG =>
        val message: PantsProtocol.Msg = PantsProtocol.Msg.parseFrom(msg.getData)
        LOGGER.info("MSG to {} : {}",message.getChannel,message.getMessage)
        val id: Int = message.getChannel
        channels.contains(id) match {
          case true =>
            channels(id).foreach { client =>
              client ! OUT(msg)
            }
        }
      case Pants.Type.PRIVMSG =>
      case Pants.Type.JOIN =>
        val join: PantsProtocol.Join = PantsProtocol.Join.parseFrom(msg.getData)
        LOGGER.info("JOIN channel {}",join.getChannel)
        val id: Int = join.getChannel.hashCode
        channels.contains(id) match {
          case true =>
            val members: Set[ActorRef] = channels(id)
            channels = channels ++ Map(id -> (members ++ Set(sender)))
          case false =>
            channels = channels ++ Map(id -> Set(sender))
        }
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
