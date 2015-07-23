package org.kndl.pants.akka

import akka.actor.{Actor, ActorRef}
import com.google.protobuf.ByteString
import org.kndl.pants.PantsProtocol._
import org.kndl.pants.{PantsProtocol, Server}
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.immutable.Map


class Dispatcher extends Actor {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Dispatcher])

  var clients: Map[Int,ActorRef] = Map()

  var names: Map[Int,ActorRef] = Map()
  var channels: Map[Int,Set[Int]] = Map()

  override def receive = {
    case in: IN =>
      handleMessage(sender(),in.msg)
  }

  def handleMessage(sender: ActorRef, msg: Pants) = {
    msg.getType match {
      case Pants.Type.LOGIN =>
        val login: PantsProtocol.Login = PantsProtocol.Login.parseFrom(msg.getData)
        handleLogin(sender,login)
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

  def handleLogin(sender: ActorRef, login: Login) = {
    login.getType match {
      case Login.Type.REQUEST =>
        login.getPassword match {
          case "password" =>
            names = names ++ Map(login.getUsername.hashCode -> sender)
            LOGGER.info("Password accepted for {}",login.getUsername)
            sender ! OUT(newLogin(Login.Type.SUCCESS,login.getUsername.hashCode))
          case _ =>
            LOGGER.info("Password failed for {}",login.getUsername)
            sender ! OUT(newLogin(Login.Type.FAIL,0))
        }
      case _ =>
        LOGGER.info("Unknown login type")
        sender ! OUT(newLogin(Login.Type.FAIL,0))
    }
  }

  def handleJoin(join: Join) = {
    channels.contains(join.getChannelId) match {
      case false =>
        channels = channels ++ Map(join.getChannelId -> Set(join.getuser))
      case true =>
    }
  }

  def ping(bytes: ByteString): Ping = {
    Ping.parseFrom(bytes)
  }

  def newLogin(loginType: Login.Type, userId: Long): Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.LOGIN)
      .setData(Login.newBuilder()
        .setType(loginType)
        .setUserId(userId)
        .build().toByteString)
      .build()
  }

  def newMsg(msg: String): Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.MSG)
      .setData(Msg.newBuilder()
      .setMessage(msg).build().toByteString)
      .build()
  }

  def newPong(timestamp: Long): Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.PONG)
      .setData(
        Pong.newBuilder()
          .setVersion(version)
          .setTimestamp(timestamp)
          .build()
          .toByteString
      ).build()
  }

  def version: Version = {
    Version.newBuilder()
      .setMajor(Server.vma)
      .setMinor(Server.vmi)
      .setPatch(Server.vpa)
      .build()
  }
}
