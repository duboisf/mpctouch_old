import javax.ws.rs._
import org.bff.javampd._
import org.bff.javampd.objects._
import org.bff.javampd.exception._
import javax.xml.bind.annotation._
 
object Mpd {

    private val mpd = new MPD("localhost", 6600)
    val player = mpd.getMPDPlayer()
}

@Path("/player")
class PlayerCommand {

  protected val player = Mpd.player
  protected val success: String = "{\"success\":true}"

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
  @Path("/song")
  @Produces(Array("application/json"))
  def doGet(): Song = new Song(player.getCurrentSong)
}

@XmlRootElement
class Song() {
  private var song: MPDSong = _

  def this(mpdsong: MPDSong) = {
    this()
    song = mpdsong
  }

  def getAlbum = song.getAlbum.getName
}

@XmlRootElement
class Album() {
  private var album: MPDAlbum = _

  def this(mpdalbum: MPDAlbum) = {
    this()
    album = mpdalbum
  }

  def getName = album.getName
}
