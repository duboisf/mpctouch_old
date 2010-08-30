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

/*
* Utility objects
*/
private[mpd] object DateFormatter {
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd G 'at' HH:mm:ss z")
  def apply(date: Date) = dateFormatter.format(date)
}

private[mpd] object ParseHelpers {
  import Function.tupled

  def parse[T <: MpdObject](lines: List[String], create: List[String] => T, mpdAttributes: List[MpdObjectAttribute#Value]): List[T] = {
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
}

/*
* MPD objects
*/
sealed abstract class MpdObject(rawData: List[String])

case class MpdSong private[mpd] (rawData: List[String]) extends MpdObject(rawData) {
}

case class MpdPlaylist private[mpd] (mpd: Mpd, private val rawData: List[String]) extends MpdObject(rawData){
  import javax.xml.bind.DatatypeConverter.parseDateTime

  if (rawData.length != 2)
    throw new IllegalArgumentException("Was expecting List of 2 strings")

  val name = rawData(0)
  val lastModified: Calendar = parseDateTime(rawData(1))

  def listSongs = mpd sendCommand(Command.ListPlaylist, name) foreach println

  def listSongsDetailed = mpd sendCommand(Command.ListPlaylistInfo, name) foreach println
}

/*
* MPD object attributes
*/
private[mpd] abstract class MpdObjectAttribute extends Enumeration {
  protected class AttributeValue(name: String, converter: String => AnyRef) extends Val(name)
}

private[mpd] object PlaylistAttribute extends MpdObjectAttribute {
  val Playlist = Value("playlist")
  val LastModified = Value("Last-Modified")
}

/*
* MPD managers
*/
class PlaylistManager private[mpd] (private val mpd: Mpd) {
  import ParseHelpers.parse
  import PlaylistAttribute.{Playlist,LastModified}

  def getPlaylists = parsePlaylists(mpd sendCommand Command.ListPlaylists)

  private def parsePlaylists(lines: List[String]): List[MpdPlaylist] = {
    parse(lines, MpdPlaylist(mpd, _), Playlist :: LastModified :: Nil)
  }
}

private[mpd] object Command extends Enumeration {
  type Command = Value

  val Close = Value("close")
  val ListPlaylists = Value("listplaylists")
  val ListPlaylist = Value("listplaylist")
  val ListPlaylistInfo = Value("listplaylistinfo")
}

class Mpd(private val _hostname: String, port: Int) {

  val host = InetAddress getByName _hostname
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

  private[mpd] def sendCommand(command: Command.Value, options: String*) = {
    val out = new BufferedWriter(new OutputStreamWriter(socket getOutputStream))
    out write command.toString
    options foreach { out write " " + _ }
    out write "\n"
    out.flush
    readOutput
  }

  private def readOutput: List[String] = {
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    def read(lines: List[String]): List[String] = {
      in.readLine match {
        case null => lines
        case line =>
          if (line startsWith "ACK")
            throw new MpdCommunicationException("Bad command: " + line)
          else if (line startsWith "OK")
            lines
          else
            read(line :: lines)
      }
    }
    read(Nil) reverse
  }

  def close {
    socket.close
  }
}

object Mpd extends Application {
  val mpd = new Mpd("127.0.0.1", 6600)
  val playlistManager = mpd.playlistManager
  val playlist = playlistManager.getPlaylists foreach { playlist =>
    println(playlist)
    playlist.listSongsDetailed
  }
  mpd.close
}
