package org.reliant.mpd

import java.io._
import java.net.{InetAddress,Socket,URI}
import java.util.Calendar
import javax.xml.bind.DatatypeConverter.parseDateTime
import scala.io.Source

class MpdException(msg: String) extends Exception(msg)
class MpdParsingException(msg: String) extends MpdException(msg)
class MpdConnectionException(msg: String) extends MpdException(msg)

case class Playlist private[mpd] (val name: String, val lastModified: Calendar)

class PlaylistManager private[mpd] (private val mpd: Mpd) {

  import ParseHelpers._

  private object Attributes extends Enumeration {
    type Attributes = Value
    val Playlist = Value("playlist")
    val LastModified = Value("Last-Modified")
  }

  def list = parsePlaylists(mpd sendCommand Command.ListPlaylists)

  def parsePlaylists(lines: List[String]): List[Playlist] = {
    lines match {
      case name :: date :: rest => {
        val parsedName = parse(name, Attributes.Playlist)
        val parsedDate = parse(date, Attributes.LastModified, parseDateTime)
        Playlist(parsedName, parsedDate) :: parsePlaylists(rest)
      }
      case "OK" :: Nil => Nil
      case data => throw new MpdParsingException("Unexpected value '" + data + "'")
    }
  }
}

private[mpd] object ParseHelpers {

  def parse[E >: Enumeration](line: String, prefix: E): String = parse(line, prefix, unit)

  def parse[T, E >: Enumeration](line: String, prefix: E, convert: String => T): T = {
    val elems = line.split(": ").toList
    if (elems.length != 2) {
      throw new MpdParsingException("Result of split not 2 elements")
    }
    if (prefix.toString != elems(0)) {
      throw new MpdParsingException("Prefix " + prefix + " doesn't match parsed prefix " + elems(0))
    }
    convert(elems(1))
  }

  def unit[T](x: T): T = x
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

  lazy val playlistManager = new PlaylistManager(this)

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
      throw new MpdConnectionException("Connection failed: " + reply)
    }
  }

  protected def reconnect {
    close
    connect
  }

  private[mpd] def sendCommand(command: Command.Value) = {
    out.println(command)
    out.flush
    readOutput
  }

  private def readOutput: List[String] = {
    def read(lines: List[String]): List[String] =
      if (in.ready) {
        val line = in.readLine()
        if (line == null) lines else read(line :: lines)
      } else {
        lines
      }
    read(List()).reverse
  }

  def close {
    out.close
    in.close
    socket.close
  }
}

object Mpd extends Application {
  val mpd = new Mpd("127.0.0.1", 6600)
  val playlistManager = mpd.playlistManager
  println("Fetching playlists")
  val playlists = playlistManager.list
  println(playlists)
  mpd.close
}
