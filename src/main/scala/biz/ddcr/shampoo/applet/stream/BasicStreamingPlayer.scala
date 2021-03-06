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

package biz.ddcr.shampoo.applet.stream

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import SoundPlayerFormat._
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine
import javazoom.jlgui.basicplayer.BasicPlayer
import javax.sound.sampled.UnsupportedAudioFileException

object BasicStreamingPlayer {
  def main(args: Array[String]) {
      val p = new BasicStreamingPlayer();
      val uri = new URI("http://server1.chilltrax.com:9000/");
      p.open(new Some(uri));
//      p.addBasicPlayerListener(arg0)
      p.play();
      println("done");
    }
}
class BasicStreamingPlayer(private val streamingOnly: Boolean = false, private var guessedType: SoundPlayerFormat = SoundPlayerFormat.UNKNOWN) extends BasicPlayer {

  private[this] val logger: Log = LogFactory.getLog(this.getClass)

  /** The JavaZoom API reads raw Objects as sources */
  protected def _open(undefinedSource: Object) {
    
    logger.debug("open(" + undefinedSource + ")")
    m_dataSource = undefinedSource
    initAudioInputStream()
  }
  def open(source: Option[Any]) {
    source match {
      case u: Some[URI] => if (u.isDefined) _open(u.get.toURL())
      case f: Some[File] => if (f.isDefined) _open(f.get)
      case None => throw new IllegalStateException("Unspecified resource identifier for playing")
      case _ => throw new UnsupportedOperationException("Unsupported resource identifier for playing, only URIs are supported") 
    }
  }

  /**
   * Inits Audio ressources from URI.
   */
  override protected def initAudioInputStream(url: URL) = {
    logger.debug("initAudioInputStream(" + url + ")")
    val typedAudioStream: TypedAudioStream = try {
      CodecSPIWorkaround.streamingAudioInputStreamAndFormat(url)      
    } catch {
      //Fallback: not a Shoutcast or an Icecast stream
      case e: UnsupportedAudioFileException => if (!streamingOnly) CodecSPIWorkaround.audioInputStreamAndFormat(url) else throw e
    }      
    m_audioInputStream = typedAudioStream.audioInputStream
    m_audioFileFormat = typedAudioStream.audioFileFormat match {
      case Some(f) => f
      case None => throw new UnsupportedAudioFileException("stream is not a supported file type")
    }
    guessedType = typedAudioStream.guessedAudioCodec
  }

  /**
   * Inits Audio ressources from InputStream.
   */
  override protected def initAudioInputStream(inputStream: InputStream) = {
    logger.debug("initAudioInputStream(" + inputStream + ")")
    //Streaming is not supported for raw streams, nor can they be typed
    val typedAudioStream: TypedAudioStream = CodecSPIWorkaround.audioInputStreamAndFormat(inputStream)
    m_audioInputStream = typedAudioStream.audioInputStream
    m_audioFileFormat = typedAudioStream.audioFileFormat match {
      case Some(f) => f
      case None => throw new UnsupportedAudioFileException("stream is not a supported file type")
    }
    guessedType = typedAudioStream.guessedAudioCodec
  }

  /**
   * Inits Audio ressources from File.
   */
  override protected def initAudioInputStream(file: File) = {
    logger.debug("initAudioInputStream(" + file + ")")
    //Streaming is not supported for plain Files
    val typedAudioStream: TypedAudioStream = CodecSPIWorkaround.audioInputStreamAndFormat(file)
    m_audioInputStream = typedAudioStream.audioInputStream
    m_audioFileFormat = typedAudioStream.audioFileFormat match {
      case Some(f) => f
      case None => throw new UnsupportedAudioFileException("file is not a supported file type")
    }
    guessedType = typedAudioStream.guessedAudioCodec
  }

  /*
   * Java mutable core code
   */
  override protected def createLine = {
    logger.debug("createLine()")
    //Java compatibility: can return null 
    if (!Option(m_line).isDefined) {
      val sourceFormat: AudioFormat = m_audioInputStream.getFormat
      logger.info("Source format: " + sourceFormat.toString)
      val nSampleSizeInBits: Int = if (
        (sourceFormat.getSampleSizeInBits != 8 & sourceFormat.getSampleSizeInBits != 24)
        ||
        (sourceFormat.getEncoding() == AudioFormat.Encoding.ULAW)
        ||
        (sourceFormat.getEncoding() == AudioFormat.Encoding.ALAW)
      ) 16 else sourceFormat.getSampleSizeInBits

      val targetFormat: AudioFormat = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate, nSampleSizeInBits, sourceFormat.getChannels, sourceFormat.getChannels
        * (nSampleSizeInBits / 8), sourceFormat.getSampleRate, false)
      logger.info("Target format: " + targetFormat)

      // Keep a reference on encoded stream for progress notification.
      m_encodedaudioInputStream = m_audioInputStream
      try {
        // Get total length in bytes of the encoded stream.
        encodedLength = m_encodedaudioInputStream.available()   
      } catch {
        case e: IOException =>
          logger.error(
            "Cannot get m_encodedaudioInputStream.available()",
            e)
      }
      try {
        //Create decoded stream
        m_audioInputStream = CodecSPIWorkaround.audioInputStream(targetFormat, m_audioInputStream, guessedType)
      } catch {
        case ex: IOException => {
            logger.error(ex)
            throw new LineUnavailableException(ex.getMessage())
          }
      }
      logger.info("Matrix-based stream decoding done")

      val audioFormat: AudioFormat = m_audioInputStream.getFormat
      val info: DataLine.Info = new DataLine.Info(classOf[SourceDataLine],
                                                  audioFormat, AudioSystem.NOT_SPECIFIED)
      val mixer: Option[Mixer] = Option(getMixer(m_mixerName))
      mixer match {
        case Some(mixer) => {
            logger.info("Mixer: " + mixer.getMixerInfo().toString())
            m_line = (mixer.getLine(info)).asInstanceOf[SourceDataLine]
          }
        case None => {
            m_line = (AudioSystem.getLine(info)).asInstanceOf[SourceDataLine]
            //Java compatibility: null
            m_mixerName = null
          }
      }
      logger.info("Line: " + m_line.toString)
      logger.debug("Line Info: " + m_line.getLineInfo.toString)
      logger.debug("Line AudioFormat: " + m_line.getFormat.toString)
    }
  }

}