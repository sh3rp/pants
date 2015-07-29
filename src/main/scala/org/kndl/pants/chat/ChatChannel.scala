package org.kndl.pants.chat

/**
 * org.kndl.pants.chat
 *
 * @author shep
 *
 **/
case class ChatChannel(id: Long, name: String, users: Set[Long])
