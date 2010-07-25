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
  def doGet(): Song = {
    if (player.getCurrentSong != null) {
      new Song(player.getCurrentSong())
    } else {
      throw new WebApplicationException(404)
    }
  }
}

class Person(private var name: String, private var age: Int) {

  def this() = this("Jane", 25)

  def getName = name
  def setName(newname: String) {
    name = newname
  }

  def getAge = age
  def setAge(newage: Int) {
    age = newage
  }
}

@XmlRootElement
class Person2(private var name: String, private var age: Int) extends Person(name, age) {
  
  def this() = this("", 0)
}

@XmlRootElement
class Song(song: MPDSong) {

  private var album = song.getAlbum().toString()
  private var artist = song.getArtist().toString()
  private var comment = song.getComment()
  private var file = song.getFile()
  private var genre = song.getGenre().toString()
  private var id = song.getId()
  private var length = song.getLength()
  private var position = song.getPosition()
  private var title = song.getTitle()
  private var track = song.getTrack()
  private var year = song.getYear()

  def this() = this(new MPDSong)

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
}

//@XmlRootElement
//class Album(album: String) extends MPDAlbum(album) {
//
//  def this() = this("")
//}
