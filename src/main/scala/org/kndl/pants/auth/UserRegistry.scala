package org.kndl.pants.auth

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import org.kndl.pants.PantsCapable
import org.kndl.pants.PantsProtocol.Pants
import org.kndl.pants.akka.OUT
import org.slf4j.{LoggerFactory, Logger}

/**
 * org.kndl.pants.auth
 *
 * @author shep
 *
 **/
class UserRegistry extends Actor with PantsCapable {

  private val LOGGER: Logger = LoggerFactory.getLogger(classOf[UserRegistry])

  var users: Map[String,User] = Map()
  var usersById: Map[Long,User] = Map()
  var userActors: Map[Long,ActorRef] = Map()

  val userIdGen: AtomicLong = new AtomicLong(1)

  override def receive: Receive = {
    case msg:AUTHORIZE =>
      LOGGER.info("Authorizing {}",msg.username)
      sender ! AUTHORIZE_RESPONSE(authorization(msg.username,msg.password,sender))
    case msg:ISAUTHORIZED =>
      LOGGER.debug("Checking user auth status - {}",msg.id)
      if(msg.id != 0) {
        sender ! ISAUTHORIZED_RESPONSE(usersById.contains(msg.id) match { case false => false case true => usersById(msg.id).authorized })
      } else {
        sender ! ISAUTHORIZED_RESPONSE(false)
      }
    case msg:Pants =>
      msg.getType match {
        case Pants.Type.MSG =>
          usersById.contains(msg.getToUserId) match {
            case true =>
              user(msg.getToUserId).actor ! OUT(newPrivateMessage(msg.getToUserId,msg.getUserId,msg.getMessage))
            case _ =>
              sender ! OUT(newError("Unknown user"))
          }
      }
  }

  def user(id: Long):User = usersById(id)

  def authorization(username: String, password: String, actor: ActorRef): User = {
    password match {
      case "password" =>
        users.contains(username) match {
          case true =>
            users += (username -> users(username).copy(authorized = true))
          case false =>
            users += (username -> User(userIdGen.getAndIncrement,username,true,actor))
        }
        users(username)
        usersById += (users(username).id -> users(username))
        userActors += (users(username).id -> actor)
      case _ =>
        users.contains(username) match {
          case true =>
            users += (username -> users(username).copy(authorized = false))
          case false =>
            users += (username -> User(userIdGen.getAndIncrement,username,false,actor))
        }
    }
    users(username)
  }

}

case class User(id: Long, name: String, authorized: Boolean, actor: ActorRef)

case class AUTHORIZE(username: String, password: String)
case class AUTHORIZE_RESPONSE(user: User)
case class ISAUTHORIZED(username: String = "", id: Long = 0)
case class ISAUTHORIZED_RESPONSE(authorized: Boolean)