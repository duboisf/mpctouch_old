package org.reliant.mpd

import java.io._
import java.net.{InetAddress,Socket,URI}
import java.util.Calendar
import javax.xml.bind.DatatypeConverter.parseDateTime
import scala.io.Source

class ParsingException(msg: String) extends Exception(msg)

case class Playlist private[mpd] (val name: String, val lastModified: String)

class PlaylistManager private[mpd] (private val mpd: Mpd) {

  private object Attributes extends Enumeration {
    type Attributes = Value
    val Playlist = Value("playlist")
    val LastModified = Value("Last-Modified")
  }

  def testParse = println(parse("playlist: test", Attributes.Playlist, unit))
  def testParse2 = println(parse("Last-Modified: 2010-06-27T20:23:18Z", Attributes.LastModified, parseDateTime))

  def list = mpd.sendCommand(Command.ListPlaylists)

  def parse[T, E >: Enumeration](line: String, prefix: E, convert: String => T): T = {
    val elems = line.split(": ").toList
    if (elems.length != 2) {
      throw new ParsingException("Result of split not 2 elements")
    }
    if (prefix.toString != elems(0)) {
      throw new ParsingException("Prefix " + prefix + " doesn't match parsed prefix " + elems(0))
    }
    convert(elems(1))
  }

  def unit[T](x: T): T = x

  def parsePlaylists(reply: List[String]): List[Playlist] = {
    def _parse(lines: List[String]): List[Playlist] = {
      lines match {
        case name :: date :: rest => {
          val playlistName = parse(name, Attributes.Playlist, unit)
          Playlist(name, date) :: _parse(rest)
        }
        case _ => Nil
      }
    }
    _parse(reply)
  }
}

private[mpd] object Helpers {
  def parse[T](reply: List[String], attributes: List[Enumeration]): List[T] = {
    null
  }
}

private[mpd] object Command extends Enumeration {
  type Command = Value

  val Close = Value("close")
  val ListPlaylists = Value("listplaylists")
}

class Mpd(private val _hostname: String, port: Int) {

  val host = InetAddress.getByName(_hostname)
  private var socket: Socket = _
  private var in: LineNumberReader = _
  private var out: PrintWriter = _

  lazy val playlists = new PlaylistManager(this)

  println("Instantiating Mpd")
  println("Host: " + host)
  println("Port: " + port)
  connect

  protected def connect {
    socket = new Socket(host, port)
    in = new LineNumberReader(new InputStreamReader(socket.getInputStream)) 
    out = new PrintWriter(socket.getOutputStream)
    val reply = readOutput
    if (reply.length != 1 || !reply(0).startsWith("OK")) {
      throw new Exception("Connection failed: " + reply)
    }
    println("Connected")
  }

  protected def reconnect {
    close
    connect
  }

  private[mpd] def sendCommand(command: Command.Value) = {
    println("Executing command " + command)
    out.println(command)
    out.flush
    readOutput
  }

  private def readOutput: List[String] = {
    def read(lines: List[String]): List[String] =
      if (in.ready) {
        val line = in.readLine()
        println(line)
        if (line == null) lines else read(line :: lines)
      } else {
        lines
      }
    read(List()).reverse
  }

  def close {
    out.print("close")
    out.close
    in.close
    socket.close
    println("Mpd connection closed")
  }
}

object Mpd extends Application {
  val mpd = new Mpd("127.0.0.1", 6600)
  val playlists = mpd.playlists
  playlists.testParse
  playlists.testParse2
  println("Fetching playlists")
  val plists = playlists.list
  println(plists)
  mpd.close
}
