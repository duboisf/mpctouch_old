package org.reliant.mpd

import java.net.{InetAddress,Socket,URI}
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
    println(Source.fromInputStream(socket.getInputStream) take 1)
  }
}
