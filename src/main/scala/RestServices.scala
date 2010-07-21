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

  @PUT
  @Produces(Array("application/json"))
  def doPut(@PathParam("command") command: String): String = {
    command match {
      case "play" => player.play()
      case "stop" => player.stop()
      case "next" => player.playNext()
      case "prev" => player.playPrev()
      case "volume" => return "{\"success\":true,\"volume\":" + player.getVolume() + "}"
      case _ => throw new WebApplicationException(400)
    }
    return success
  }

  @PUT
  @Path("/{value: [0-9]{0,3}}")
  @Produces(Array("application/json"))
  def doPost( @PathParam("command") cmd: String,
              @PathParam("value") value: Int ): String = {
    if (cmd != "volume" || value > 100 || value < 0 ) {
      throw new WebApplicationException(400)
    }
    player.setVolume(value)
    return success
  }

  @GET
  @Produces(Array("application/json"))
  def doGet(@PathParam("command") command: String): String = {
    if (command != "volume") {
      throw new WebApplicationException(400)
    }
    return "{\"success\":true,\"volume\":" + player.getVolume() + "}"
  }
}

@Path("/json")
class JSONTest {

  @GET
  @Produces(Array("application/json"))
  def doGet: String = return "{\"success\":true}"
}

@XmlRootElement
class User() {

  private var name: String = _

  def this(_name: String) {
    this()
    name = _name
  }

  def getName = name
  def setName(_name: String) = name = _name
}
