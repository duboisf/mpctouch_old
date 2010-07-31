package org.reliant.mpctouch

import javax.ws.rs._
import org.bff.javampd._
import org.bff.javampd.objects._
import org.bff.javampd.exception._
import javax.xml.bind.annotation._
import com.sun.jersey.spi.resource.Singleton
import collection.JavaConversions._
import collection.mutable._
import org.atmosphere.annotation.Suspend
 
object Mpd {

    var mpd = new MPD("localhost", 6600)
    val player = mpd.getMPDPlayer()
    def playlist = new MPD("localhost", 6600).getMPDPlaylist()
}

@Path("/player")
class Player {

  private val player = Mpd.player
  private val success: String = "{\"success\":true}"

  @PUT
  @Path("/command/{command}")
  @Produces(Array("application/json"))
  def doPutCommand(@PathParam("command") command: String): String = {
    command match {
      case "play" => player.play()
      case "stop" => player.stop()
      case "next" => player.playNext()
      case "prev" => player.playPrev()
      case _ => throw new WebApplicationException(400)
    }
    return success
  }

  @PUT
  @Path("/volume/{value: [0-9]{0,3}}")
  @Produces(Array("application/json"))
  def doPutVolume(@PathParam("value") value: Int): String = {
    if (value > 100) {
      throw new WebApplicationException(400)
    }
    player.setVolume(value)
    return success
  }

  @GET
  @Path("/volume")
  @Produces(Array("application/json"))
  def doGetVolume() = "{\"success\":true,\"volume\":" + player.getVolume() + "}"

  @GET
  @Path("/song/current")
  @Produces(Array("application/json"))
  def doGet(): Song = {
    if (player.getCurrentSong != null) {
      new Song(player.getCurrentSong())
    } else {
      throw new WebApplicationException(404)
    }
  }
}

@Path("/playlist")
class Playlist {

  private def playlist = Mpd.playlist

  @GET
  @Path("/songs")
  @Produces(Array("application/json"))
  def doGetSongList = new Songs(playlist.getSongList())
}

@Path("/comet")
@Singleton
class Comet {

  @GET
  @Suspend
  def suspend = ""

}

@XmlRootElement
class Songs() {

  private var songs: java.util.List[Song] = new java.util.ArrayList()

  def this(mpdsongs: java.util.List[MPDSong]) {
    this()
    val it = mpdsongs.iterator
    while (it.hasNext) {
      songs.add(new Song(it.next))
    }
  }

  def getSongs = songs
  def setSongs(newsongs: java.util.List[Song]) = songs = newsongs
}

@XmlRootElement
class Song(song: MPDSong) {

  private var album = safeToString(song.getAlbum())
  private var artist = safeToString(song.getArtist())
  private var comment = song.getComment()
  private var file = song.getFile()
  private var genre = safeToString(song.getGenre())
  private var id = song.getId()
  private var length = song.getLength()
  private var position = song.getPosition()
  private var title = song.getTitle()
  private var track = song.getTrack()
  private var year = song.getYear()

  def this() = this(new MPDSong)

  def safeToString(x: AnyRef): String =
    if (x != null) {
      x.toString()
    } else {
      ""
    }

  def getAlbum = album
  def setAlbum(newalbum: String) {
    album = newalbum
  }
  def getArtist = artist
  def setArtist(newartist: String) {
    artist = newartist
  }
  def getComment = comment
  def setComment(newcomment: String) {
    comment = newcomment
  }
  def getFile = file
  def setFile(newfile: String) {
    file = newfile
  }
  def getGenre = genre
  def setGenre(newgenre: String) {
    genre = newgenre
  }
  def getId = id
  def setId(newid: Int) {
    id = newid
  }
  def getLength = length
  def setLength(newlength: Int) {
    length = newlength
  }
  def getPosition = position
  def setPosition(newposition: Int) {
    position = newposition
  }
  def getTitle = title
  def setTitle(newtitle: String) {
    title = newtitle
  }
  def getTrack = track
  def setTrack(newtrack: Int) {
    track = newtrack
  }
  def getYear = year
  def setYear(newyear: String) {
    year = newyear
  }

  override def toString = List(artist, title, album).reduceLeft(_ + " - " + _)

}

