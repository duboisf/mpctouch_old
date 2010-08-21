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
import javax.xml.bind.annotation.{XmlSeeAlso, XmlRootElement}
import java.util.concurrent.Executors
import org.atmosphere.jersey.{Broadcastable, JerseyBroadcaster}
import java.util.Date

trait Listener {
  def notify(event: String): Event[AnyRef]
}

object Mpd extends PlayerBasicChangeListener {

  {
    mpd = new MPD("localhost", 6600)
    val monitor = new MPDStandAloneMonitor(mpd)
    monitor.addPlayerChangeListener(this)
    val thread = new Thread(monitor)
    thread.start
  }

  var mpd: MPD = _
  def player = mpd.getMPDPlayer()
  def playlist = mpd.getMPDPlaylist()
  val success: String = "{\"success\":true}"

  override def playerBasicChange(event: PlayerBasicChangeEvent) = {
    val msg = event.getId() match {
      case PlayerBasicChangeEvent.PLAYER_STARTED => dispatch("play")
      case PlayerBasicChangeEvent.PLAYER_STOPPED => dispatch("stop")
      case _ => "case not dealt with"
    }
    val bc = BroadcasterFactory.getDefault.lookup(classOf[JerseyBroadcaster], classOf[JerseyBroadcaster].getSimpleName, true)
    if (bc != null) {
      val ts = new Date
      println("current ts: " + ts.getTime.toString)
      bc.broadcast("{\"success\":true,\"currentTime\": " + ts.getTime.toString + "}").get
      bc.resumeAll
    } else {
      println("got null broadcaster")
    }
    println("player event ids " + event.getId())
  }

  def dispatch(event: String) {
//    event match {
//      case "play" => {
//        Event(event, currentSong)
//      }
//      case "stop" => {
//        Event(event, null)
//      }
//      case "next" => {
//        Event(event, currentSong)
//      }
//      case "prev" => {
//        Event(event, currentSong)
//      }
//    }
  }
}

@XmlRootElement
@XmlSeeAlso(Array(classOf[Song]))
case class Event[T >: Null <: AnyRef](event: String, data: T) {

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

// Test
@Path("/{topic}")
class PubSub {

  @GET
  @Suspend(resumeOnBroadcast = true)
  def suspend(): Broadcastable = {
    val bc = new JerseyBroadcaster()
    new Broadcastable(bc)
  }
}

@Path("/player")
class Player extends Listener {

  private def player = Mpd.player
  private val success = Mpd.success
  private def playlist = Mpd.playlist

  @Broadcast(resumeOnBroadcast = true)
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def notify(event: String): Event[AnyRef] = {
    println("notified")
    event match {
      case "play" => {
        Event(event, currentSong)
      }
      case "stop" => {
        Event(event, null)
      }
      case "next" => {
        Event(event, currentSong)
      }
      case "prev" => {
        Event(event, currentSong)
      }
    }
  }

  @GET
  @Suspend(resumeOnBroadcast = true)
  def suspend(): Broadcastable = {
    println("oh ho! a get request!")
    val bc = new JerseyBroadcaster()
    return new Broadcastable(bc)
  }

  @PUT
  @Path("/command/{command}")
  @Broadcast(resumeOnBroadcast = true)
  @Produces(Array(MediaType.APPLICATION_JSON))
  def execCommand(@PathParam("command") command: String): Event[AnyRef] = {
    command match {
      case "play" => {
        player.play()
        notify("play")
      }
      case "stop" => {
        player.stop()
        notify("stop")
      }
      case "next" => {
        player.playNext()
        notify("next")
      }
      case "prev" => {
        player.playPrev()
        notify("prev")
      }
      case _ => throw new WebApplicationException(400)
    }
  }

  @PUT
  @Path("/volume/{value: [0-9]{0,3}}")
  @Broadcast
  @Produces(Array(MediaType.APPLICATION_JSON))
  def doPutVolume(@PathParam("value") value: Int): Event[String] = {
    if (value > 100) {
      throw new WebApplicationException(400)
    }
    player.setVolume(value)
    return currentVolume
  }

  @GET
  @Path("/volume")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def currentVolume(): Event[String] = {
    val volume: Int = player.getVolume()
    return new Event("volume", volume.toString())
  }

  @GET
  @Path("/song/current")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def currentSong(): Song = {
    if (player.getCurrentSong != null) {
      new Song(player.getCurrentSong())
    } else {
      throw new WebApplicationException(404)
    }
  }

  @GET
  @Path("/playlist/songs")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getSongList = new Songs(playlist.getSongList())

  @PUT
  @Path("/playlist/play")
  @Broadcast
  @Produces(Array(MediaType.APPLICATION_JSON))
  def playIndex(@FormParam("index") index: Int) {
    val songs = playlist.getSongList()
    if (index >= songs.size) {
      throw new WebApplicationException(404)
    }
    player.playId(songs.get(index))
    notify("play")
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

