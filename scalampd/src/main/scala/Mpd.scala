package org.reliant.mpd

import java.net.{InetAddress,Socket,URI}
import java.io.DataOutputStream
import scala.io.Source

class Mpd(private val _hostname: String, port: Int) {

  val host = InetAddress.getByName(_hostname)
  private var socket: Socket = _

  {
    println("Instantiating Mpd")
    println("Host: " + host)
    println("Port: " + port)
    connect
  }

  private def connect {
    socket = new Socket(host, port)
    val in = Source.fromInputStream(socket.getInputStream)
    println("Connected")
  }

  private def close = socket.close

}

object Test extends Application {
  val mpd = new Mpd("127.0.0.1", 6600)
  println("Exiting?")
}
