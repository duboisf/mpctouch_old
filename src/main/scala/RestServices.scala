import javax.ws.rs._
import org.bff.javampd._
import org.bff.javampd.exception._
import javax.xml.bind.annotation._
 
object Mpd {

    private val mpd = new MPD("localhost", 6600)
    val player = mpd.getMPDPlayer()
}

@Path("/player/{command}")
class Player {

  protected val player = Mpd.player
  protected val success: String = "{\"success\":true}"

  @GET
  @Produces(Array("application/json"))
  def doGet(@PathParam("command") command: String): String = {
    command match {
      case "play" => player.play()
      case "stop" => player.stop()
      case "next" => player.playNext()
      case "prev" => player.playPrev()
      case "volume" => return "{\"success\":true,\"volume\":" + player.getVolume() + "}"
      case _ => throw new MPDPlayerException("Unknown command")
    }
    return success
  }

  @POST
  @Produces(Array("application/json"))
  def doPost( @PathParam("command") cmd: String,
              @FormParam("value") value: Int ): String = {
    if (cmd != "volume") {
      throw new MPDPlayerException("Unknown command")
    }
    if (value > 100 || value < 0) {
      throw new MPDPlayerException("volume value must be specified by int between 0 and 100")
    }
    player.setVolume(value)
    return success
  }
}

//trait PlayerService {
//
//  protected val player = Mpd.player
//  protected val success = "{success:true}"
//  protected val failure = "{success:false}"
//
//  @GET @Produces(Array("application/json"))
//  def doGet: String
//}
//
//@Path("/player/stop")
//class Stop extends PlayerService {
//
//  def doGet: String = {
//    player.stop()
//    return success
//  }
//}
//
//@Path("/player/play")
//class Play extends PlayerService {
//
//  private var paused = false
//
//  override def doGet: String = {
////    if (paused) {
//      player.play()
////    } else {
////      player.pause()
////    }
////    paused = paused == false
//    return success
//  }
//}
//
//@Path("/player/next")
//class Next extends PlayerService {
//
//  override def doGet: String = {
//    player.playNext()
//    return success
//  }
//}
//
//@Path("/player/prev")
//class Previous extends PlayerService {
//
//  override def doGet: String = {
//    player.playPrev()
//    return success
//  }
//}
//
//@Path("/volup")
//class VolumeUp extends PlayerService {
//
//  override def doGet: String = {
//    val vol = player.getVolume
//    if (vol <= 95) {
//      player.setVolume(vol + 5);
//    }
//    return success
//  }
//}
//
//@Path("/voldown")
//class VolumeDown extends PlayerService {
//
//  override def doGet: String = {
//    val vol = player.getVolume
//    if (vol > 5) {
//      player.setVolume(vol - 5);
//    }
//    return success
//  }
//}
//
//@Path("/vol/{value}")
//class Volume extends PlayerService {
//
//  override def doGet: String = {
//    val vol = player.getVolume()
//    return "{success:true,volume:" + vol + "}"
//  }
//
//  @POST
//  @Produces(Array("application/json"))
//  def doSet(@PathParam("value") value: Int): String = {
//    if (value < 0 || value > 100) {
//      return failure
//    }
//    player.setVolume(value)
//    return success
//  }
//}
//
//@Path("/json")
//class JSONTest {
//
//  @GET
//  @Produces(Array("application/json"))
//  def doGet: User = return new User("Fred")
//}
//
//@XmlRootElement
//class User() {
//
//  private var name: String = _
//
//  def this(newname: String) = {
//    this()
//    name = newname
//  }
//
//  def getName = name
//  def setName(newname: String) = name = newname
//}
