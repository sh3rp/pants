package org.kndl.pants.auth

import java.util.concurrent.atomic.AtomicLong

import akka.actor.Actor
import akka.actor.Actor.Receive
import org.slf4j.{LoggerFactory, Logger}

/**
 * org.kndl.pants.auth
 *
 * @author shep
 *
 **/
class Authenticator extends Actor {

  private val LOGGER: Logger = LoggerFactory.getLogger(classOf[Authenticator])

  var users: Map[String,User] = Map()
  var usersById: Map[Long,User] = Map()

  val userIdGen: AtomicLong = new AtomicLong(1)

  override def receive: Receive = {
    case msg:AUTHORIZE =>
      LOGGER.info("Authorizing {}",msg.username)
      sender ! AUTHORIZE_RESPONSE(authorization(msg.username,msg.password))
    case msg:ISAUTHORIZED =>
      if(msg.id != 0) {
        sender ! ISAUTHORIZED_RESPONSE(usersById.contains(msg.id) match { case false => false case true => usersById(msg.id).authorized })
      } else {
        sender ! ISAUTHORIZED_RESPONSE(false)
      }
  }

  def authorization(username: String, password: String): User = {
    password match {
      case "password" =>
        users.contains(username) match {
          case true =>
            users += (username -> users(username).copy(authorized = true))
          case false =>
            users += (username -> User(userIdGen.getAndIncrement,username,true))
        }
        users(username)
        usersById += (users(username).id -> users(username))
      case _ =>
        users.contains(username) match {
          case true =>
            users += (username -> users(username).copy(authorized = false))
          case false =>
            users += (username -> User(userIdGen.getAndIncrement,username,false))
        }
    }
    users(username)
  }

}

case class User(id: Long, name: String, authorized: Boolean)

case class AUTHORIZE(username: String, password: String)
case class AUTHORIZE_RESPONSE(user: User)
case class ISAUTHORIZED(username: String = "", id: Long = 0)
case class ISAUTHORIZED_RESPONSE(authorized: Boolean)