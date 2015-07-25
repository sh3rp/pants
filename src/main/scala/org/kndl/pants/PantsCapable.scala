package org.kndl.pants

import org.kndl.pants.PantsProtocol.Pants

/**
 * org.kndl.pants
 *
 * @author shep
 *
 **/
trait PantsCapable {

  def newLoginRequest(username: String, password: String):Pants = {
    newPants(Pants.Type.LOGIN_REQUEST)
      .setUsername(username)
      .setPassword(password)
      .build()
  }

  def newLoginResponse(userId: Long, loggedIn: Boolean):Pants = {
    newPants(Pants.Type.LOGIN_RESPONSE)
      .setUserId(userId)
      .setLoggedIn(loggedIn)
      .build
  }

  def newPing:Pants = {
    newPants(Pants.Type.PING)
      .build()
  }

  def newPong(timestamp: Long):Pants = {
    Pants.newBuilder()
      .setType(Pants.Type.PONG)
      .setTimestamp(timestamp)
      .setVersionMajor(Version.major)
      .setVersionMinor(Version.minor)
      .setVersionPatch(Version.patch)
      .build
  }

  def newJoinRequest(channel: String, userId: Long): Pants = {
    newPants(Pants.Type.JOIN_REQUEST)
      .setUserId(userId)
      .setChannelName(channel)
      .build()
  }

  def newJoinResponse(channel: String, channelId: Long): Pants = {
    newPants(Pants.Type.JOIN_RESPONSE)
      .setChannelName(channel)
      .setChannelId(channelId)
      .build()
  }

  def newMessage(channelId: Long, userId: Long, msg: String): Pants = {
    newPants(Pants.Type.MSG)
      .setUserId(userId)
      .setChannelId(channelId)
      .setMessage(msg)
      .build()
  }

  def newPants(packetType: Pants.Type): Pants.Builder = {
    Pants.newBuilder()
      .setType(packetType)
      .setTimestamp(System.currentTimeMillis())
      .setVersionMajor(Version.major)
      .setVersionMinor(Version.minor)
      .setVersionPatch(Version.patch)
  }

}
