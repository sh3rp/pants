package org.kndl.pants.netty.client

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.kndl.pants.{PantsCapable, Client, PantsProtocol}
import org.kndl.pants.PantsProtocol._
import org.slf4j.{Logger, LoggerFactory}

/**
 *
 *
 * @author shep
 *
 **/
class ClientHandler extends SimpleChannelInboundHandler[Pants] with PantsCapable {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[ClientHandler])

  var ctx: ChannelHandlerContext = _
  var username: String = _
  var userId: Long = _
  var channelsNameToId: Map[String,Long] = Map()
  var channelsIdToName: Map[Long,String] = Map()
  var users: Map[String,Long] = Map()

  def loggedIn:Boolean = userId != 0

  def channelJoined(channelName: String): Boolean = channelsNameToId.contains(channelName)

  def sendLogin(username: String, password: String) = {
    this.username = username
    ctx.writeAndFlush(newLoginRequest(username,password))
  }

  def sendPing() = {
    ctx.writeAndFlush(newPing)
  }

  def sendMsg(channel: String, message: String) = {
    if(channelsNameToId.contains(channel)) {
      LOGGER.info("UserID {} sending msg",userId)
      ctx.writeAndFlush(newMessage(channelsNameToId(channel), userId, message))
    }
  }

  def sendJoinRequest(channel: String) = {
    LOGGER.info("Using userId {} to send join",userId)
    ctx.writeAndFlush(newJoinRequest(channel,userId))
  }

  def sendPrivMsg(user: String, message: String) = {
    ctx.writeAndFlush(newPrivateMessage())
  }

  override def channelRegistered(ctx: ChannelHandlerContext) = {
    this.ctx = ctx
  }

  override def channelRead0(channelHandlerContext: ChannelHandlerContext, msg: Pants): Unit = {
    msg.getType() match {
      case PantsProtocol.Pants.Type.LOGIN_RESPONSE =>
        msg.getLoggedIn match {
          case true => userId = msg.getUserId
            LOGGER.debug("Login for user {} succeeded, userId = {}",username,msg.getUserId)
          case false => userId = 0
            LOGGER.debug("Login for user {} failed",msg.getUserId)
        }
      case PantsProtocol.Pants.Type.PONG =>
        LOGGER.info("PONG v{}.{}.{} ({}ms)",
          Integer.toString(msg.getVersionMajor),
          Integer.toString(msg.getVersionMinor),
          Integer.toString(msg.getVersionPatch),
          (System.currentTimeMillis() - msg.getTimestamp).toString)
      case PantsProtocol.Pants.Type.MSG =>
        LOGGER.info("<{}:{}> {}",channelsIdToName(msg.getChannelId),"lol",msg.getMessage)
      case PantsProtocol.Pants.Type.PRIVMSG =>
        LOGGER.info("[{}] {}",msg.getUserId,msg.getMessage)
      case PantsProtocol.Pants.Type.JOIN_RESPONSE =>
        msg.getChannelId match {
          case 0 =>
            LOGGER.info("Joining channel {} failed",msg.getChannelName)
          case chanId: Long =>
            LOGGER.info("This Channel {} registered to id {}",msg.getChannelName,msg.getChannelId)
            channelsNameToId = channelsNameToId ++ Map(msg.getChannelName -> chanId)
            channelsIdToName = channelsIdToName ++ Map(chanId -> msg.getChannelName)
            LOGGER.info("Channels = {}",channelsNameToId)
        }
      case _ =>
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, c: Throwable): Unit = {
    c.printStackTrace()
  }
}