package org.reliant.mpctouch

import javax.ws.rs.{GET,PUT,Produces,Path,PathParam,FormParam,WebApplicationException}
import org.bff.javampd.MPD
import org.bff.javampd.objects.MPDSong
import org.bff.javampd.events.{PlayerChangeListener,PlayerChangeEvent}
import javax.xml.bind.annotation.XmlRootElement
import com.sun.jersey.spi.resource.Singleton
import org.atmosphere.annotation.{Suspend,Broadcast}
import org.atmosphere.annotation.Suspend.SCOPE
import org.atmosphere.cpr.Meteor
 
object Mpd extends PlayerChangeListener {

  {
    println("Hello?")
    mpd = new MPD("localhost", 6600)
    player.addPlayerChangeListener(this)
  }

  var mpd: MPD = _
  def player = mpd.getMPDPlayer()
  def playlist = mpd.getMPDPlaylist()
  val success: String = "{\"success\":true}"

  override def playerChanged(event: PlayerChangeEvent) = {
    print("player change event: ")
    val msg = event.getId() match {
      case PlayerChangeEvent.PLAYER_NEXT => "next song"
      case PlayerChangeEvent.PLAYER_PREVIOUS => "prev song"
      case _ => "case not dealt with"
    }
    println(msg)
  }
}

@Path("/player")
class Player {

  private def player = Mpd.player
  private val success = Mpd.success

  @PUT
  @Path("/command/{command}")
  @Broadcast
  @Produces(Array("application/json"))
  def execCommand(@PathParam("command") command: String): Song = {
    command match {
      case "play" => player.play()
      case "stop" => player.stop()
      case "next" => player.playNext()
      case "prev" => player.playPrev()
      case _ => throw new WebApplicationException(400)
    }
    return currentSong()
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
  def currentSong(): Song = {
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
  private val success = Mpd.success
  private def player = Mpd.player

  @GET
  @Path("/songs")
  @Produces(Array("application/json"))
  def doGetSongList = new Songs(playlist.getSongList())

  @PUT
  @Path("/play")
  @Produces(Array("application/json"))
  def playIndex(@FormParam("index") index: Int): String = {
    val songs = playlist.getSongList()
    if (index >= songs.size) {
      throw new WebApplicationException(404)
    }
    player.playId(songs.get(index))
    return success
  }
}

@Path("/comet")
@Singleton
class Comet {

  @Path("/suspend")
  @Suspend(resumeOnBroadcast = true, scope = SCOPE.VM)
  @GET
  @Produces(Array("application/json"))
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

