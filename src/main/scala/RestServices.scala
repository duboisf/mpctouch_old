package org.reliant.mpctouch

import com.sun.jersey.spi.resource.Singleton
import javax.ws.rs.{GET,PUT,Produces,Path,PathParam,FormParam,WebApplicationException}
import javax.ws.rs.core.MediaType
import org.atmosphere.annotation.{Suspend,Broadcast,Resume}
import org.atmosphere.cpr.{BroadcasterFactory,Broadcaster}
import org.bff.javampd.MPD
import org.bff.javampd.events.{PlayerBasicChangeListener,PlayerBasicChangeEvent}
import org.bff.javampd.monitor.MPDStandAloneMonitor
import org.bff.javampd.objects.MPDSong
import java.util.concurrent.Executors
import org.atmosphere.jersey.{Broadcastable, JerseyBroadcaster}
import java.util.Date
import javax.xml.bind.annotation.{XmlElement, XmlSeeAlso, XmlRootElement}

trait Listener {
  def notify(event: String): Event[AnyRef]
}

object Mpd extends PlayerBasicChangeListener {

  {
    println("Mpd object instantiating")
    mpd = new MPD("localhost", 6600)
    val monitor = new MPDStandAloneMonitor(mpd)
    monitor.addPlayerChangeListener(this)
    val thread = new Thread(monitor)
    thread.start
  }

  var mpd: MPD = _
  val success = "{\"success\":true}"
  def player = mpd.getMPDPlayer()
  def playlist = mpd.getMPDPlaylist()

  override def playerBasicChange(event: PlayerBasicChangeEvent) = {
    println("player change event: " + event.getId)
    val msg = event.getId() match {
      case PlayerBasicChangeEvent.PLAYER_STARTED => dispatch("play")
      case PlayerBasicChangeEvent.PLAYER_STOPPED => dispatch("stop")
      case _ => "case not dealt with"
    }
  }

  def currentSong(): Song = {
    if (player.getCurrentSong != null) {
      new Song(player.getCurrentSong())
    } else {
      throw new WebApplicationException(404)
    }
  }

  def buildSuccess(data: String) = "{\"success\":true," + data + "}"

  def dispatch(eventName: String) {
    val event = eventName match {
      case "play" => {
        Event(eventName, currentSong)
      }
      case "stop" => {
        Event(eventName, null)
      }
      case "next" => {
        Event(eventName, currentSong)
      }
      case "prev" => {
        Event(eventName, currentSong)
      }
      case "volume" => {
        Event(eventName, "\"volume\":" + player.getVolume)
      }
      case "playlist->play" => {
        Event(eventName, currentSong)
      }
      case "playlist->songs" => {
        Event(eventName, new Songs(playlist.getSongList()))
      }
    }
    val bc = BroadcasterFactory.getDefault.lookup(classOf[JerseyBroadcaster], classOf[JerseyBroadcaster].getSimpleName)
    if (bc != null) {
      bc.broadcast(event)
    } else {
      println("dispatch: null broadcaster")
    }
  }
}

//@XmlRootElement
//case class VolumeEvent[String] extends Event[String] {
//
//}

//trait AEvent[T >: Null <: AnyRef] {
//  private var _event = event
//}

@XmlRootElement
@XmlSeeAlso(Array(classOf[Song]))
case class Event[T >: Null <: AnyRef](_event: String, _data: T) {

  @XmlElement
  var event = _event
  @XmlElement
  private var data = _data
  @XmlElement
  private var success = true

  // Need default constructor for JAXB
  def this() = this("", null)

//  def getSuccess = _success
//  def setSuccess(status: Boolean) {
//    _success = status
//  }
  
//  def getEvent = _event
//  def setEvent(name: String) {
//    _event = name
//  }

//  def getData = _data
//  def setData(data: T) {
//    _data = data
//  }
}

@Path("/player")
class Player {

  import Mpd.player
  import Mpd.playlist
  import Mpd.dispatch
  import Mpd.success
  import Mpd.buildSuccess

  @GET
  @Suspend(resumeOnBroadcast = true)
  @Produces(Array(MediaType.APPLICATION_JSON))
  def suspend(): Broadcastable = {
    println("oh ho! a get request!")
    val bc = new JerseyBroadcaster()
    return new Broadcastable(bc)
  }

  @PUT
  @Path("/command/{command}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def execCommand(@PathParam("command") command: String) = {
    command match {
      case "play" => {
        player.play()
      }
      case "stop" => {
        player.stop()
      }
      case "next" => {
        player.playNext()
        dispatch("next")
      }
      case "prev" => {
        player.playPrev()
        dispatch("prev")
      }
      case _ => throw new WebApplicationException(400)
    }
    success
  }

  @PUT
  @Path("/volume/{value: [0-9]{0,3}}")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def doPutVolume(@PathParam("value") value: Int) = {
    if (value > 100) {
      throw new WebApplicationException(400)
    }
    player.setVolume(value)
    dispatch("volume")
    success
  }
  
  @PUT
  @Path("/playlist/play")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def playIndex(@FormParam("index") index: Int) = {
    val songs = playlist.getSongList()
    if (index >= songs.size) {
      throw new WebApplicationException(404)
    }
    player.playId(songs.get(index))
    success
  }
  
  @GET
  @Path("/volume")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def currentVolume = buildSuccess("\"volume\":" + player.getVolume)

  @GET
  @Path("/song/current")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getCurrentSong =
    if (player.getCurrentSong != null) {
      new Song(player.getCurrentSong)
    } else {
      throw new WebApplicationException(404)
    }

  @GET
  @Path("/playlist/songs")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getSongList = new Songs(playlist.getSongList())
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

