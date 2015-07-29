package org.kndl.pants.akka

import java.util.concurrent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorSelection, Actor, ActorRef}
import akka.util.Timeout
import com.google.protobuf.ByteString
import org.kndl.pants.PantsProtocol._
import org.kndl.pants.auth.{ISAUTHORIZED_RESPONSE, ISAUTHORIZED}
import org.kndl.pants.{PantsCapable, PantsProtocol, Server}
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.immutable.Map
import akka.pattern.ask

import scala.concurrent.Await


class Dispatcher extends Actor with PantsCapable {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Dispatcher])

  val authenticator: ActorSelection = context.actorSelection("../authenticator")

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
    isAuthorized(msg.getUserId) match {
      case true =>
        msg.getType match {
          case Pants.Type.MSG =>
            handleMsg(sender, msg)
          case Pants.Type.PRIVMSG =>
          case Pants.Type.JOIN_REQUEST =>
            handleJoin(sender, msg)
          case _ =>
        }
      case _ =>
        sender ! OUT(newLoginResponse(msg.getUserId,false))
    }
  }

  def isAuthorized(userId: Long): Boolean = {
    val future = authenticator ? ISAUTHORIZED(username = "",userId)
    val result = Await.result(future, timeout.duration).asInstanceOf[ISAUTHORIZED_RESPONSE]
    result.authorized
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
