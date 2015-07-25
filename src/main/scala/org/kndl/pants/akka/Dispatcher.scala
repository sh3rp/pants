package org.kndl.pants.akka

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorRef}
import com.google.protobuf.ByteString
import org.kndl.pants.PantsProtocol._
import org.kndl.pants.{PantsCapable, PantsProtocol, Server}
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.immutable.Map


class Dispatcher extends Actor with PantsCapable {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Dispatcher])

  var clients: Map[Long,ActorRef] = Map()

  var names: Map[Long,String] = Map()
  var channels: Map[Long,Set[Long]] = Map()

  val userIdGen: AtomicLong = new AtomicLong(1)
  val channelIdGen: AtomicLong = new AtomicLong(1)

  def newUserId: Long = {
    userIdGen.getAndIncrement()
  }
  
  def newChannelId: Long = {
    channelIdGen.getAndIncrement()
  }

  override def receive = {
    case in: IN =>
      handlePacket(sender(),in.msg)
  }

  def handlePacket(sender: ActorRef, msg: Pants) = {
    msg.getType match {
      case Pants.Type.LOGIN_REQUEST =>
        handleLogin(sender,msg)
      case Pants.Type.MSG =>
        handleMsg(sender,msg)
      case Pants.Type.PRIVMSG =>
      case Pants.Type.JOIN_REQUEST =>
        handleJoin(sender,msg)
      case _ =>
    }
  }

  def handleLogin(sender: ActorRef, login: Pants) = {
    login.getPassword match {
      case "password" =>
        val newId: Long = newUserId
        clients = clients ++ Map(newId -> sender)
        names = names ++ Map(newId -> login.getUsername)
        LOGGER.info("Password accepted for {} ({})", login.getUsername, newId)
        sender ! OUT(newLoginResponse(newId,true))
      case _ =>
        LOGGER.info("Password failed for {}", login.getUsername)
        sender ! OUT(newLoginResponse(0,false))
    }
  }

  def handleMsg(sender: ActorRef, msg: Pants) = {
    if(channels.contains(msg.getChannelId) && names.contains(msg.getUserId)) {
      LOGGER.info("<{}:{}> {}", channels(msg.getChannelId), names(msg.getUserId), msg.getMessage)
      channels(msg.getChannelId).foreach { clientId =>
        LOGGER.info("Sending message to {}",clientId)
        LOGGER.info("clients = {}",clients)
        clients(clientId) ! OUT(newMessage(msg.getChannelId,msg.getUserId,msg.getMessage))
      }
    }
  }

  def handleJoin(sender: ActorRef,join: Pants) = {
    join.getChannelId match {
      case 0 =>
        LOGGER.info("Creating channel {} for userId {}",join.getChannelName,join.getUserId)
        val channelId = newChannelId
        channels = channels ++ Map(channelId -> Set(join.getUserId))
        LOGGER.info("Sending join response: {}",channelId)
        sender ! OUT(newJoinResponse(join.getChannelName,channelId))
      case id: Long => channels.contains(id) match {
        case true =>
          LOGGER.info("Adding user {} to channel {}", join.getUserId, join.getChannelId)
          var users: Set[Long] = channels(join.getChannelId)
          users = users ++ Set(join.getUserId)
          channels = channels ++ Map(join.getChannelId -> users)
          channels(join.getChannelId).foreach { client =>
            clients(client) ! OUT(newJoinResponse(join.getChannelName, join.getChannelId))
          }
        case _ =>
          LOGGER.info("Unknown channel id {}",join.getChannelId)
      }
    }
  }
}
