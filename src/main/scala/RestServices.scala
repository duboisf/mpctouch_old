package org.reliant.mpctouch

import com.sun.jersey.spi.resource.Singleton
import javax.ws.rs.{GET,PUT,Produces,Path,PathParam,FormParam,WebApplicationException}
import org.atmosphere.annotation.{Suspend,Broadcast,Resume}
import org.atmosphere.cpr.{BroadcasterFactory,Broadcaster}
import org.atmosphere.jersey.JerseyBroadcaster
import org.bff.javampd.events.{PlayerChangeListener,PlayerChangeEvent}
import org.bff.javampd.MPD
import org.bff.javampd.objects.MPDSong
import javax.xml.bind.annotation.{XmlSeeAlso, XmlRootElement}

object Mpd extends PlayerChangeListener {

  {
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

@XmlRootElement
@XmlSeeAlso(Array(classOf[Song]))
class Event[T >: Null <: AnyRef](event: String, data: T) {

  private var _event = event
  private var _data = data

  def this() = this("", null)

  def getEvent = _event
  def setEvent(name: String) {
    _event = name
  }

  def getData = _data
  def setData(data: T) {
    _data = data
  }
}

@Path("/player")
class Player {

  private def player = Mpd.player
  private val success = Mpd.success
  private def playlist = Mpd.playlist

  @GET
  @Suspend(resumeOnBroadcast = true, scope = Suspend.SCOPE.VM )
  @Produces(Array("application/json"))
  def suspend: Event[AnyRef] = null

  @PUT
  @Path("/command/{command}")
  @Broadcast(resumeOnBroadcast = true)
  @Produces(Array("application/json"))
  def execCommand(@PathParam("command") command: String): Event[Song] = {
    command match {
      case "play" => {
        player.play()
        new Event("play", currentSong)
      }
      case "stop" => {
        player.stop()
        new Event("stop", null)
      }
      case "next" => {
        player.playNext()
        new Event("next", currentSong)
      }
      case "prev" => {
        player.playPrev()
        new Event("prev", currentSong)
      }
      case _ => throw new WebApplicationException(400)
    }
  }

  @PUT
  @Path("/volume/{value: [0-9]{0,3}}")
  @Broadcast
  @Produces(Array("application/json"))
  def doPutVolume(@PathParam("value") value: Int): Event[String] = {
    if (value > 100) {
      throw new WebApplicationException(400)
    }
    player.setVolume(value)
    return currentVolume
  }

  @GET
  @Path("/volume")
  @Produces(Array("application/json"))
  def currentVolume(): Event[String] = {
    val volume: Int = player.getVolume()
    return new Event("volume", volume.toString())
  }

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

  @GET
  @Path("/playlist/songs")
  @Produces(Array("application/json"))
  def getSongList = new Songs(playlist.getSongList())

  @PUT
  @Path("/playlist/play")
  @Broadcast
  @Produces(Array("application/json"))
  def playIndex(@FormParam("index") index: Int): Song = {
    val songs = playlist.getSongList()
    if (index >= songs.size) {
      throw new WebApplicationException(404)
    }
    player.playId(songs.get(index))
    return currentSong
  }
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

