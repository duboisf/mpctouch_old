package org.bff.javampd

import exception.{MPDPlaylistException, MPDResponseException}
import objects.MPDSong
import java.util.ArrayList


/**
 * Created by IntelliJ IDEA.
 * User: fred
 * Date: 21-Aug-2010
 * Time: 1:55:06 PM
 * To change this template use File | Settings | File Templates.
 */

class MyMPD(server: String, port: Int, passwd: String) extends MPD(server, port, passwd) {
  
  def this(server: String) = this(server, 6600, null)

  def this(server: String, port: Int) = {
    this(server, port, null)
    println("Instantiated MyMPD")
  }

  override def convertResponseToSong(response: java.util.List[String]) = super.convertResponseToSong(response)
}

class Playlist(_mpd: MyMPD) extends MPDPlaylist(_mpd) {

  def fetchSongList = {
    val command = new MPDCommand("MPD_PLAYLIST_LIST")
    var response: ArrayList[String] = null
    
    try {
      response = new ArrayList[String](_mpd.sendMPDCommand(command))
    }
    catch {
      case re: MPDResponseException => throw new MPDPlaylistException(re.getMessage, re.getCommand)
      case e: Exception => throw new MPDPlaylistException(e.getMessage)
    }

    val list = new ArrayList[MPDSong](_mpd.convertResponseToSong(response))

    list
  }

}