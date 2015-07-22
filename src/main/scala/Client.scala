import java.io.FileReader
import java.util.Properties

import com.kndl.pants.netty.ClientInitializer
import io.netty.bootstrap.Bootstrap
import io.netty.channel.socket.SocketChannel
import io.netty.channel._
import io.netty.channel.nio.{NioEventLoopGroup, NioEventLoop}
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.{ProtobufEncoder, ProtobufVarint32LengthFieldPrepender, ProtobufDecoder, ProtobufVarint32FrameDecoder}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{SslContextBuilder, SslContext}
import jline.console.ConsoleReader
import org.kndl.pants.PantsProtocol
import org.kndl.pants.PantsProtocol.{Msg, Ping, Pong, Pants}
import org.kndl.pants.netty.client.{ClientInitializer, ClientHandler}
import org.slf4j.{Logger, LoggerFactory}


object Client extends App {

  val HOST: String = "127.0.0.1"
  val PORT: Int = 8463

  var ctx: SslContext = SslContextBuilder.forClient()
    .trustManager(InsecureTrustManagerFactory.INSTANCE).build()

  val group: EventLoopGroup = new NioEventLoopGroup()
  try {
    val b: Bootstrap = new Bootstrap()
    b.group(group).channel(classOf[NioSocketChannel]).handler(new ClientInitializer(ctx))
    val ch: Channel = b.connect(HOST,PORT).sync().channel()
    val handler: ClientHandler = ch.pipeline().get[ClientHandler](classOf[ClientHandler])
    val con = new ConsoleReader()
/*    while(true) {
      con.readCharacter() match {
        case 49 =>
          println("sending ping")
          handler.sendPing()
        case 50 =>
          println("sending msg")
          handler.sendMsg("OHAI!")
        case _ =>
      }
    }*/
    for(x <- 1 to 10) {
      handler.sendPing()
    }
    Thread.sleep(5000)
    handler.sendMsg("Hello!")
    Thread.sleep(1000)
    handler.sendMsg("Good morning!")
  } finally {
    group.shutdownGracefully()
  }

}




