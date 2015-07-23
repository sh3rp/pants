package org.kndl.pants.netty.client

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol._
import org.slf4j.{Logger, LoggerFactory}

/**
 *
 *
 * @author shep
 *
 **/
class ClientHandler extends SimpleChannelInboundHandler[Pants] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[ClientHandler])

  var ctx: ChannelHandlerContext = _
  var userId: Int = _
  var channels:Map[String,Int] = Map()

  def sendLogin(username: String, password: String) = {
    ctx.writeAndFlush(login(username,password))
  }

  def sendPing() = {
    ctx.writeAndFlush(ping)
  }

  def sendMsg(channel: String, message: String) = {
    ctx.writeAndFlush(msg(channel,message))
  }

  def sendJoin(user: String, channel: String) = {
    ctx.writeAndFlush(join(channel))
  }

  override def channelRegistered(ctx: ChannelHandlerContext) = {
    this.ctx = ctx
  }

  override def channelRead0(channelHandlerContext: ChannelHandlerContext, msg: Pants): Unit = {
    msg.getType() match {
      case PantsProtocol.Pants.Type.LOGIN =>
        val login: Login = PantsProtocol.Login.parseFrom(msg.getData)
        login.getType match {
          case Login.Type.SUCCESS =>
            userId = login.getUserId
          case Login.Type.FAIL =>
            userId = 0
        }
      case PantsProtocol.Pants.Type.PONG =>
        val pong: Pong = PantsProtocol.Pong.parseFrom(msg.getData)
        LOGGER.info("PONG v{}.{}.{} ({}ms)",
          Integer.toString(pong.getVersion().getMajor),
          Integer.toString(pong.getVersion().getMinor),
          Integer.toString(pong.getVersion.getPatch),
          (System.currentTimeMillis() - pong.getTimestamp).toString)
      case PantsProtocol.Pants.Type.MSG =>
        val message: Msg = PantsProtocol.Msg.parseFrom(msg.getData)
        LOGGER.info("<{}> {}",message.getChannel,message.getMessage)
      case PantsProtocol.Pants.Type.JOIN =>
        val join: Join = PantsProtocol.Join.parseFrom(msg.getData)

      case _ =>
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, c: Throwable): Unit = {
    c.printStackTrace()
  }

  def login(username: String, password: String):Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.LOGIN)
      .setData(Login.newBuilder().setUsername(username).setPassword(password).setType(Login.Type.REQUEST).build().toByteString)
      .build()
  }

  def ping:Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.PING)
      .setData(Ping.newBuilder().setTimestamp(System.currentTimeMillis()).build().toByteString).build()
  }

  def join(channel: String): Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.JOIN)
      .setData(Join.newBuilder().setUser(userId).setChannelId(channel.hashCode).build().toByteString)
      .build()
  }

  def msg(channel: String, msg: String):Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.MSG)
      .setData(Msg.newBuilder().setChannel(channel.hashCode).setMessage(msg).build().toByteString)
      .build()
  }
}