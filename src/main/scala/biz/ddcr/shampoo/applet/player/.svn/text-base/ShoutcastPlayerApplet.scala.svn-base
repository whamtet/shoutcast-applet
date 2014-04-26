/*
 *  Copyright (C) 2012 okay_awright <okay_awright AT ddcr DOT biz>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.ddcr.shampoo.applet.player

import scala.Predef._
import swing.event.{ MouseReleased, MouseExited, MouseEntered }
import swing._
import biz.ddcr.shampoo.applet.misc.HTMLColour
import java.awt.{ Graphics, Color, Cursor }
import scala.Long
import javazoom.jlgui.basicplayer._
import biz.ddcr.shampoo.applet.misc.ImageScaler
import org.apache.commons.logging.{ LogFactory, Log }
import java.net.URI
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import java.io.FileInputStream
import java.io.IOException
import java.net.MalformedURLException
import java.io.File
import java.net.URL
import biz.ddcr.shampoo.applet.stream.BasicStreamingPlayer
import biz.ddcr.shampoo.applet.stream.StreamingEndpoint
import scala.util.control.TailCalls._

object PlayerState extends Enumeration {
  type PlayerState = Value
  val PLAYING, BUFFERING, STOPPED = Value
}
import PlayerState._

import biz.ddcr.shampoo.applet.misc.URIFixer._

class ShoutcastPlayerApplet extends Applet {

  private[this] val logger: Log = LogFactory.getLog(this.getClass)

  //GUI
  private lazy val playImage: ImageIcon = initializeImage("playingImage", "resources/play.gif")
  private lazy val stopImage: ImageIcon = initializeImage("stoppedImage", "resources/stop.gif")
  private lazy val bufferImage: ImageIcon = initializeImage("bufferingImage", "resources/buffer.gif")
  private lazy val playOverImage: ImageIcon = initializeImage("playingImageOver", "resources/playOver.gif")
  private lazy val stopOverImage: ImageIcon = initializeImage("stoppedImageOver", "resources/stopOver.gif")
  private lazy val bufferOverImage: ImageIcon = initializeImage("bufferingImageOver", "resources/bufferOver.gif")
  private lazy val backgroundColour: Color = initializeColour("backgroundColour", "white")

  private lazy val fakeButton: Label = new Label

  //Sound engine
  private var state: PlayerState = STOPPED
  private lazy val endpoint: StreamingEndpoint = initializeEndpoint("endpoint", "charset", "http://overfitted.ddcr.biz/radio.php?.pls")
  private lazy val player: BasicStreamingPlayer = new BasicStreamingPlayer
  private lazy val listener: BasicPlayerListener = new BasicPlayerListener {
    
    def opened(p1: Any, p2: java.util.Map[_, _]) {
      /*Do nothing*/
    }
    
    def progress(p1: Int, p2: Long, p3: Array[Byte], p4: java.util.Map[_, _]) {
      /*Do nothing*/
    }

    def stateUpdated(bpe: BasicPlayerEvent) {
      //Java compatibility: can return null
      if (bpe != null) bpe.getCode match {
        case BasicPlayerEvent.EOM => {
            logger.debug("State updated: EOM")
            state = STOPPED
          }
        case BasicPlayerEvent.OPENING => {
            logger.debug("State updated: opening")
            state = BUFFERING
          }
        case BasicPlayerEvent.OPENED => {
            logger.debug("State updated: opened")
            player.play
          }
        case BasicPlayerEvent.PLAYING => {
            logger.debug("State updated: playing")
            state = PLAYING
          }
        case BasicPlayerEvent.STOPPED => {
            logger.debug("State updated: stopped")
            state = STOPPED
          }
        case x => logger.debug("Unsupported operation "+x)
      }
      refreshButton
    }

    def setController(p1: BasicController) {
      /*Do nothing*/
    }
  }

  private def initializeColour(parameterName: String, defaultValue: String): Color = {
    HTMLColour.decode(
      //Java compatibility: can return null
      Option(Option(getParameter(parameterName)).getOrElse(defaultValue))
    ) match {
      case Some(decodedColour) => decodedColour
      case None => Color.WHITE
    }
  }

  private def initializeImage(parameterName: String, defaultValue: String): ImageIcon = {
    val inputStream =
      //Java compatibility: can return null
    Option(getParameter(parameterName)) match {
      case Some(f) => {
          val parameterURI = resolveFileURIIfAny(new URI(f))
          parameterURI.getScheme.toLowerCase match {
            case "file" => new FileInputStream(parameterURI.getPath)
            case _ => parameterURI.toURL.openStream
          }
        }              
      case None => this.getClass.getClassLoader.getResourceAsStream(defaultValue)
    }
    try {
      new ImageIcon(
        ImageScaler.scale(
          ImageIO.read(
            inputStream
          ),
          getWidth,
          getHeight))
    } finally{
      inputStream.close
    }
  }

  private def initializeEndpoint(parameterName1: String, parameterName2: String, defaultValue: String): StreamingEndpoint = {
    val currentPlaylist: String = Option(getParameter(parameterName1)).getOrElse(defaultValue)   
    logger.debug("Loading endpoint: " + currentPlaylist)
    //Java compatibility: can return null
    Option(getParameter(parameterName2)) match {
      case Some(charset) => StreamingEndpoint.parse(new URI(currentPlaylist), charset)
      case None => StreamingEndpoint.parse(new URI(currentPlaylist))
    }    
  }

  private def refreshButton {
    fakeButton.icon = (
      state match {
        case PLAYING => playImage
        case BUFFERING => bufferImage
        case _ => stopImage
      })
    repaint()
  }

  private def refreshOverButton {
    fakeButton.icon = (
      state match {
        case PLAYING => playOverImage
        case BUFFERING => bufferOverImage
        case _ => stopOverImage
      })
    repaint()
  }

  object ui extends UI with Reactor {
    override def init: Unit = {
      state = STOPPED
      try {

        player.addBasicPlayerListener(listener)
        listener.setController(player)

        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        fakeButton.opaque = false
        fakeButton.background = backgroundColour
        refreshButton
        contents = fakeButton

        //Mouse listener
        //Capture mouse events from the fake button 
        listenTo(fakeButton.mouse.clicks, fakeButton.mouse.moves)
        reactions += {
          case e: MouseEntered => {
              //Called when the pointer enters the applet's rectangular area
              refreshOverButton
              e.consume
            }
          case e: MouseExited => {
              //Called when the pointer leaves the applet's rectangular area
              refreshButton
              e.consume
            }
          case e: MouseReleased => {
              //Called after a mouse click on the applet is released
              if (togglePlaying) {
                refreshOverButton
                e.consume
              }
            }

        }

      } catch {
        case e: Exception => logger.error("Initialization failed", e)
      }

    }
  }

  private def togglePlaying: Boolean = {
    if (state != STOPPED) stopStream else checkNextStream.result
  }

  //Generic Events
  /** Play the registered stream*/
  private def checkNextStream: TailRec[Boolean] = {    
    if (endpoint.hasNext) {
      //Let's check each URI entry of the endpoint until it can play or give up
      val localURI = endpoint.next
      try {
        //URL or plain local file?
        try {
          player.open(new URL(localURI.toString))
        } catch {
          case e: MalformedURLException =>
            //It should be a local file then
            player.open(new File(localURI))
        }
        done(true)
      } catch {
        case e: Exception =>
          logger.error("Cannot read file: " + localURI, e)
          //Force state reinitialisation, Javazoom usually got stuck in BUFFERING state
          state = STOPPED
          refreshButton
          //try to play the next URI in the endpoint
          tailcall(checkNextStream)
      }
    } else {
      //Rewind the endpoint playlist
      endpoint.rewind
      done(false)
    }
  }
  /** Stop the registered stream*/
  private def stopStream: Boolean = {
    try {
      player.stop
      //Rewind the endpoint playlist
      endpoint.rewind
      true
    } catch {
      case e: Exception =>
        logger.error("Stop failed", e)
        false
    }
  }

  override def paint(g: Graphics) {
    super.paint(g)
    g.setColor(backgroundColour)
  }
}
