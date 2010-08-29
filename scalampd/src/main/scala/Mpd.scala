package org.reliant.mpd

import java.io._
import java.net.{InetAddress,Socket,URI}
import java.util.{Calendar,Date}
import java.text.SimpleDateFormat
import javax.xml.bind.DatatypeConverter.parseDateTime
import scala.io.Source

class MpdException(msg: String) extends Exception(msg)
class MpdParsingException(msg: String) extends MpdException(msg)
class MpdConnectionException(msg: String) extends MpdException(msg)
class MpdCommunicationException(msg: String) extends MpdException(msg)

private[mpd] object DateFormatter {
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd G 'at' HH:mm:ss z")
  def apply(date: Date) = dateFormatter.format(date)
}

sealed abstract class MpdObject(lines: List[String])

case class MpdPlaylist private[mpd] (lines: List[String]) extends MpdObject(lines){
  import javax.xml.bind.DatatypeConverter.parseDateTime
  val name = lines(0)
  val lastModified: Calendar = parseDateTime(lines(1))
}

case class OldMpdPlaylist private[mpd] (val name: String, val lastModified: Calendar) {
  override def toString = "Playlist name: " + name + ", last modified date: " + DateFormatter(lastModified.getTime)
}

private[mpd] abstract class MpdObjectAttribute extends Enumeration {
  protected class AttributeValue(name: String, converter: String => AnyRef) extends Val(name)
}

private[mpd] object PlaylistAttribute extends MpdObjectAttribute {
  val Playlist = Value("playlist")
  val LastModified = Value("Last-Modified")
}

class PlaylistManager private[mpd] (private val mpd: Mpd) {
  import ParseHelpers.parse
  import PlaylistAttribute.{Playlist,LastModified}

  def list = parsePlaylists(mpd sendCommand Command.ListPlaylists)

  def parsePlaylists(lines: List[String]): List[MpdPlaylist] = {
    parse(lines, List(Playlist, LastModified), MpdPlaylist(_))
  }
}

private[mpd] object ParseHelpers {

  def parse[T <: MpdObject](lines: List[String], mpdAttributes: List[MpdObjectAttribute#Value], create: List[String] => T): List[T] = {
    import Function.tupled
    def _parse(linesToParse: List[String]): List[T] = {
      if (linesToParse == Nil) {
        Nil
      } else {
        val (lines, rest) = linesToParse splitAt mpdAttributes.length
        val parsedLines = lines zip mpdAttributes map tupled { parseLine(_, _) }
        create(parsedLines) :: _parse(rest)
      }
    }
    _parse(lines) reverse
  }

  private def parseLine(line: String, expectedAttribute: MpdObjectAttribute#Value): String = {
    val attributeName :: attributeValue :: Nil = line split ": " toList;
    if (attributeName == expectedAttribute.toString)
      attributeValue
    else
      throw new MpdParsingException("Was expecting line to start with '" + expectedAttribute + "' but started with '" + attributeName + "' instead")
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

  lazy val playlistManager = new PlaylistManager(this)

  println("Instantiating Mpd")
  println("Host: " + host)
  println("Port: " + port)
  connect

  protected def connect {
    socket = new Socket(host, port)
    val reply = readOutput
    if (reply != Nil) {
      throw new MpdConnectionException("Connection failed: " + reply)
    }
  }

  protected def reconnect {
    close
    connect
  }

  private[mpd] def sendCommand(command: Command.Value) = {
    val out = socket.getOutputStream
    out.write((command + "\n").getBytes)
    readOutput
  }

  private def readOutput: List[String] = {
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    def read(lines: List[String]): List[String] = {
      in.readLine match {
        case null => lines
        case line =>
          if (line startsWith "ACK")
            throw new MpdCommunicationException("Bad command")
          else if (line startsWith "OK")
            lines
          else
            read(line :: lines)
      }
    }
    read(List()).reverse
  }

  def close {
    socket.close
  }
}

object Mpd extends Application {
  val mpd = new Mpd("127.0.0.1", 6600)
  val playlistManager = mpd.playlistManager
  println("Saved playlists:")
  playlistManager.list foreach println
  mpd.close
}
