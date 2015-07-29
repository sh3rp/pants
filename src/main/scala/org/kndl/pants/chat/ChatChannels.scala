package org.kndl.pants.chat

import java.util.concurrent.atomic.AtomicLong

import akka.actor.Actor
import akka.actor.Actor.Receive

/**
 * org.kndl.pants.chat
 *
 * @author shep
 *
 **/
class ChatChannels extends Actor {

  var channels: Map[String,ChatChannel] = Map()
  var channelsById: Map[Long,ChatChannel] = Map()

  val channelIdGen: AtomicLong = new AtomicLong(1)

  override def receive: Receive = {
    case msg: NEW_CHANNEL =>
      sender ! NEW_CHANNEL(msg.name,addChannel(msg.name).id)
    case msg: JOIN_CHANNEL =>
    case msg: USERS =>
    case msg: USERS_LIST =>
    case _ =>
  }

  def addChannel(name: String): ChatChannel = {

  }
}

case class NEW_CHANNEL(name: String, id: Long = 0)
case class JOIN_CHANNEL(channelId: Long, userId: Long)
case class USERS(channelId: Long)
case class USERS_LIST(channelId: Long, users: Set[Long])