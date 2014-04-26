/*
 *  Copyright (C) 2012 okay_awright <okay_awright AT ddcr DOT biz>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package biz.ddcr.shampoo.applet.stream

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import java.net.URLConnection
import java.io.InputStream
import SoundPlayerFormat._
import biz.ddcr.shampoo.applet.misc.GenericUserAgent
import biz.ddcr.shampoo.applet.misc.HTTPHeader
import java.io.BufferedInputStream
import javax.sound.sampled.AudioFileFormat
import java.io.File
import java.io.FileInputStream
import javax.sound.sampled.UnsupportedAudioFileException
import java.net.URL
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioFormat
import java.net.Socket
import java.io.OutputStream
import javazoom.spi.mpeg.sampled.file.tag.IcyInputStream
import javazoom.spi.mpeg.sampled.file.IcyListener
import java.io.PrintWriter
import java.io.FileNotFoundException

/**
 * Check what SPI codecs are available at runtime and use them
 * Streaming fix for Shoutcast/Icecast
 * TODO: make all MP4/AAC VBR tracks playable, not just CBR
 */
object CodecSPIWorkaround {
  
  def spit(str: String) {
    try {
    val MyFileTxtTarget = new PrintWriter("/Users/matthewmolloy/clojure/chilltrax/header.txt")
 
    MyFileTxtTarget.print(str)
 
    MyFileTxtTarget.close()
  } catch {
    case e: FileNotFoundException => println(e.getLocalizedMessage())
    case e: Throwable => {
      println("Some other exception type:")
      e.printStackTrace()
    }
  }
  }
  def main(args: Array[String]) {
      println(HTTP_GET_ICY_HEADER);
    }

  case class TypedStream(icyStream: InputStream, format: SoundPlayerFormat) {}
  
  private[this] val logger: Log = LogFactory.getLog(this.getClass)

  private final val MARK_READ_LIMIT: Int = 1000
  private lazy final val USER_AGENT: String = (new GenericUserAgent {
      protected def compatibilityClient: Option[VersionizedComponent] = {
        //Impersonate header for WinAMP
        Some(new VersionizedComponent(Some("WinampMPEG"), Some("2.7")))
      }
      override protected def modules: Option[Iterable[Option[VersionizedComponent]]] = {
        Option(List(
            try {
              val clazz = Class.forName("javazoom.jlgui.basicplayer.BasicPlayer")
              //Dummy package inspection to beat the smart optimization techniques that prevent the package from being loaded as long as no item from it is accessed
              val dummy = clazz.asInstanceOf[javazoom.jlgui.basicplayer.BasicPlayer].hashCode
              Some(new VersionizedComponent(
                  Some("BasicPlayer"),
                  GenericUserAgent.classVersion(clazz)))
            } catch {
              //Silently fails
              case e: Exception => None
            },
            try {
              val clazz = Class.forName("net.sourceforge.jaad.spi.javasound.AACAudioFileReader")
              //Dummy package inspection to beat the smart optimization techniques that prevent the package from being loaded as long as no item from it is accessed
              val dummy = clazz.asInstanceOf[net.sourceforge.jaad.spi.javasound.AACAudioFileReader].hashCode
              Some(new VersionizedComponent(
                  Some("jAAD"),
                  GenericUserAgent.classVersion(clazz)))
            } catch {
              //Silently fails
              case e: Exception => None
            },
            try {
              val clazz = Class.forName("org.kc7bfi.jflac.sound.spi.FlacAudioFileReader")
              //Dummy package inspection to beat the smart optimization techniques that preventthe package from being loaded as long as no item from it is accessed
              val dummy = clazz.asInstanceOf[org.kc7bfi.jflac.sound.spi.FlacAudioFileReader].hashCode
              Some(new VersionizedComponent(
                  Some("jFLAC"),
                  GenericUserAgent.classVersion(clazz)))
            } catch {
              //Silently fails
              case e: Exception => None
            },
            try {
              val clazz = Class.forName("javazoom.jl.player.Player")
              //Dummy package inspection to beat the smart optimization techniques that preventthe package from being loaded as long as no item from it is accessed
              val dummy = clazz.asInstanceOf[javazoom.jl.player.Player].hashCode
              Some(new VersionizedComponent(
                  Some("JL"),
                  GenericUserAgent.classVersion(clazz)))
            } catch {
              //Silently fails
              case e: Exception => None
            },
            try {
              val clazz = Class.forName("javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader")
              //Dummy package inspection to beat the smart optimization techniques that preventthe package from being loaded as long as no item from it is accessed
              val dummy = clazz.asInstanceOf[javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader].hashCode
              Some(new VersionizedComponent(
                  Some("jOrbis"),
                  GenericUserAgent.classVersion(clazz)))
            } catch {
              //Silently fails
              case e: Exception => None
            },

            try {
              val clazz = Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader")
              //Dummy package inspection to beat the smart optimization techniques that prevent the package from being loaded as long as no item from it is accessed
              val dummy = clazz.asInstanceOf[javazoom.spi.mpeg.sampled.file.MpegAudioFileReader].hashCode
              Some(new VersionizedComponent(
                  Some("MP3SPI"),
                  GenericUserAgent.classVersion(clazz)))
            } catch {
              //Silently fails
              case e: Exception => None
            }))
      }

    }).banner

  private lazy final val HTTP_GET_ICY_HEADER: String = ("User-Agent: " :: USER_AGENT :: "\r\n"
                                                        :: "Accept: " :: "*/*" :: "\r\n"
                                                        :: "Icy-Metadata: " :: "1" :: "\r\n"
                                                        :: "Connection: " :: "close" :: "\r\n"
                                                        :: "\r\n" :: Nil).mkString

  def defaultReadOptions(urlConnection: URLConnection): URLConnection = {
    urlConnection.setUseCaches(false)
    urlConnection.setDoInput(true)
    urlConnection.setDoOutput(false)
    urlConnection.setAllowUserInteraction(false)
    urlConnection.setConnectTimeout(10000)
    urlConnection.setReadTimeout(10000)
    urlConnection.setRequestProperty("Connection", "Close")
    urlConnection.setRequestProperty("User-Agent", USER_AGENT)
    urlConnection
  }

  def keepAliveReadOptions(urlConnection: URLConnection): URLConnection = {
    urlConnection.setUseCaches(false)
    urlConnection.setDoInput(true)
    urlConnection.setDoOutput(false)
    urlConnection.setAllowUserInteraction(false)
    urlConnection.setConnectTimeout(10000)
    urlConnection.setReadTimeout(10000)
    urlConnection.setRequestProperty("User-Agent", USER_AGENT)
    urlConnection
  }

  def audioInputStreamAndFormat(inputStream: InputStream): TypedAudioStream = {
    logger.debug("getAudioInputStreamAndFormat(" + inputStream + ")")
    //No type can be guessed from raw streams 
    audioInputStreamAndFormat(inputStream, SoundPlayerFormat.UNKNOWN)
  }

  /** Peak the header of a resource and go */
  def readAndReset[A](i: InputStream, j: Int, f: InputStream => A): A = {
    val bufferedI: InputStream = if (!i.markSupported) new BufferedInputStream(i) else i
    try {
      bufferedI.mark(j)
      f(bufferedI)
    } finally {
      bufferedI.reset
    }
  }

  def audioInputStreamAndFormat(inputStream: InputStream, format: SoundPlayerFormat): TypedAudioStream = {
    logger.debug("getAudioInputStreamAndFormat(" + inputStream + "," + format + ")")
    val guessedFormat = readAndReset(inputStream, MARK_READ_LIMIT, r => Option(audioFileFormat(r, format)))
    //FIX: Don't browse all default SPI providers first, the Tritonus MPEG provider can sometimes crash with MP4, perform this after all pre-defined options have been exhausted first
    //Don't use partially applied functions since properly resetting the stream in a timely manner is vital
    new TypedAudioStream(
      format,
      audioInputStream(inputStream, format),
      guessedFormat)
  }

  def audioInputStreamAndFormat(file: File): TypedAudioStream = {
    logger.debug("getAudioInputStreamAndFormat(" + file + ")")
    //Check type
    val format: SoundPlayerFormat = checkFileExtension(fileExtension(Option(file.getPath)))
    val inputStream: InputStream = new FileInputStream(file)
    audioInputStreamAndFormat(inputStream, format)
  }

  def audioInputStreamAndFormat(url: URL): TypedAudioStream = {
    logger.debug("getAudioInputStreamAndFormat(" + url + ")")
    
    //Don't use the URL if it's a plain file
    if (url.getProtocol.toLowerCase.equals("file")) {
      audioInputStreamAndFormat(new File(url.toURI))
    } else {
    
      val urlConnection: URLConnection = defaultReadOptions(url.openConnection)
      //Check type
      val format: SoundPlayerFormat =
        try {
          checkHttpMimeType(urlConnection)
        } catch {
          case e: UnsupportedAudioFileException =>
            //Silently fails, try the next mime type detection mechanism
            checkFileExtension(Option(url.getFile))
        }
      val inputStream: InputStream = defaultReadOptions(url.openConnection).getInputStream
      audioInputStreamAndFormat(inputStream, format)
      
    }
  }

  def streamingAudioInputStreamAndFormat(url: URL): TypedAudioStream = {
    logger.debug("getShoutcastAudioInputStreamAndFormat(" + url + ")")

    //Only HTTP is acceptable for streaming at the moment
    url.getProtocol.toLowerCase match {
      case "http" => Unit
      case _ => throw new UnsupportedAudioFileException("Only HTTP is suitable for streaming at the moment")
    }    
    
    /**
     * Test HTTP headers (no official specifications): SHOUTCAST icy-notice1
     * = <BR>This stream requires <a
     * href="http://www.winamp.com/">Winamp</a><BR> icy-notice2 = SHOUTcast
     * Distributed Network Audio Server/Linux v1.9.8<BR> icy-name =
     * Overfitting Disco icy-genre = Disco icy-url =
     * http://overfitted.ddcr.biz/ content-type = audio/mpeg icy-pub = 1
     * icy-br = 128
     *
     * ICECAST Content-Type = audio/mpeg icy-br = 128,128 ice-audio-info =
     * ice-samplerate=44100ice-bitrate=128ice-channels=2 icy-genre = Disco
     * icy-name = Overfitting Disco icy-pub = 1 icy-url =
     * http://overfitted.ddcr.biz/ Server = Icecast 2.3.2-kh30 Cache-Control
     * = no-cache Expires = Mon, 26 Jul 1997 05:00:00 GMT Pragma = no-cache
     */
    // Tell shoutcast server (if any) that SPI support shoutcast stream.               
    val maxIcyTagLength: Int = 4

    //Don't use URLConnection with Java 7 because of the twisted Shoutcast-HTTP protocol, just a plain socket
    val socket: Socket = new Socket(url.getHost, url.getPort)
    val os: OutputStream = socket.getOutputStream
    val header = ("GET " :: (if (url.getPath.length < 1) "/" else url.getFile) :: " " :: "HTTP/1.0" :: "\r\n" :: HTTP_GET_ICY_HEADER :: Nil).mkString
    
    os.write(header.getBytes)

    val bufferedInputStream = new BufferedInputStream(socket.getInputStream)
    val isShout = readAndReset(
      bufferedInputStream,
      maxIcyTagLength,
      r => {
        var head: Array[Byte] = new Array[Byte](maxIcyTagLength)
        val read: Int = r.read(head, 0, maxIcyTagLength)
        //Looking for the magic ICY header
        //Trivia: Shoutcast codename, before release, was I Can Yell
        (read > 2) && (((head(0) == 'I') | (head(0) == 'i')) && ((head(1) == 'C') | (head(1) == 'c')) && ((head(2) == 'Y') | (head(2) == 'y')))
      })
    
    val icyTypedStream: TypedStream = {
      // Is it a shoutcast server ?
      if (isShout) {
        logger.info("Shoutcast stream detected")
        val stream = new IcyInputStream(bufferedInputStream)
        //Attach tag parser
        stream.asInstanceOf[IcyInputStream].addTagParseListener(IcyListener.getInstance)
        new TypedStream(
          stream,
          try {
            checkMimeType(Option(stream.getTag("content-type").getValue.asInstanceOf[String]))
          } catch {
            case e: UnsupportedAudioFileException =>
              //Silently fails, try the next mime type detection mechanism
              checkFileExtension(Option(url.getFile))
          })
      } else {
        //not shoutcast? then it must be Icecast (HTTP 200 Ok instead of ICY 200 OK)
        //Just by detecting if Metadata is on should be enough to determine whether it's a real audio stream
        val httpHeader = new HTTPHeader(bufferedInputStream)

        if (!(httpHeader.httpResponse.map(_.matches(".*200 +O[Kk]")).getOrElse(false)))
          throw new UnsupportedAudioFileException("Invalid Icecast HTTP stream")
        val metaInt = httpHeader.entry("icy-metaint")
        if (!metaInt.isDefined)
          // Otherwise just give up, it must be a regular HTTP text-based stream
        throw new UnsupportedAudioFileException("Unknown or unsupported type of stream")

        logger.info("Icecast stream detected")

        val stream = new IcyInputStream(bufferedInputStream, metaInt.get)
        //Attach tag parser
        stream.asInstanceOf[IcyInputStream].addTagParseListener(IcyListener.getInstance)
        new TypedStream(
          stream,
          try {
            checkMimeType(httpHeader.entry("content-type"))
          } catch {
            case e: UnsupportedAudioFileException =>
              //Silently fails, try the next mime type detection mechanism
              checkFileExtension(Option(url.getFile))
          })
      }
    }

    //Don't use partially applied functions since properly resetting the stream in a timely manner is vital
    val forcedFormat = readAndReset(bufferedInputStream, MARK_READ_LIMIT, r => Option(audioFileFormat(r, icyTypedStream.format)))
    //You cannot pass the IcyInputStream to getAudioFileFormat otherwise you will lose the Tag metapackets, only a classic bufferedInputStream         
    new TypedAudioStream(
      icyTypedStream.format,
      audioInputStream(icyTypedStream.icyStream, icyTypedStream.format),
      forcedFormat
    )
  }

  def audioFileFormat(sourceStream: InputStream, format: SoundPlayerFormat): AudioFileFormat = {
    logger.debug("getAudioFileFormat(" + sourceStream + "," + format + ")")
    //FIX: Don't browse all default SPI providers first, the Tritonus MPEG provider can sometimes crash with MP4, perform this after all pre-defined options have been exhausted first
    val f: Option[AudioFileFormat] = try {
      Option(forcedAudioFileFormat(sourceStream, format))
    } catch {
      //Do nothing, try next detection mechanism
      case e: UnsupportedAudioFileException => { logger.info("Forced audio detection failed due to lack of codec: failsafe mode", e); None }
      case e: ClassNotFoundException => { logger.info("Forced audio detection failed due to lack of codec: failsafe mode", e); None }
    }
    f match {
      case Some(f) => f
      case None => AudioSystem.getAudioFileFormat(sourceStream)
    }
  }

  def audioInputStream(sourceStream: InputStream, format: SoundPlayerFormat): AudioInputStream = {
    logger.debug("getAudioFileStream(" + sourceStream + "," + format + ")")
    //FIX: Don't browse all default SPI providers first, the Tritonus MPEG provider can sometimes crash with MP4, perform this after all pre-defined options have been exhausted first
    val s: Option[AudioInputStream] = try {
      Option(forcedAudioInputStream(sourceStream, format))
    } catch {
      //Do nothing, try next detection mechanism
      case e: UnsupportedAudioFileException => { logger.info("Forced streaming failed due to lack of codec: failsafe mode",e); None }
      case e: ClassNotFoundException => { logger.info("Forced streaming failed due to lack of codec: failsafe mode", e); None }
    }
    s match {
      case Some(s) => s
      case None => AudioSystem.getAudioInputStream(sourceStream)
    }
  }

  def audioInputStream(targetFormat: AudioFormat, sourceStream: AudioInputStream, format: SoundPlayerFormat): AudioInputStream = {
    logger.debug("getAudioInputStream(" + targetFormat + "," + sourceStream + "," + format + ")")
    //FIX: Don't browse all default SPI providers first, the Tritonus MPEG provider can sometimes crash with MP4, perform this after all pre-defined options have been exhausted first
    val s: Option[AudioInputStream] = try {
      Option(forcedAudioInputStream(targetFormat, sourceStream, format))
    } catch {
      //Do nothing, try next detection mechanism
      case e: UnsupportedAudioFileException => { logger.info("Forced streaming failed due to lack of codec: failsafe mode",e); None }
      case e: ClassNotFoundException => { logger.info("Forced streaming failed due to lack of codec: failsafe mode",e); None }
    }
    s match {
      case Some(s) => s
      case None => AudioSystem.getAudioInputStream(targetFormat, sourceStream)
    }
  }

  protected def checkMimeType(mimeType: Option[String]): SoundPlayerFormat = {
    logger.debug("getCheckMimeType("+mimeType+")")
    mimeType match {
      case Some(mime) => {
          mime.toLowerCase match {
            case "audio/aac" | "audio/aacp" => SoundPlayerFormat.RAW_AAC
            case "audio/mp4" | "application/mp4" | "audio/x-mp4" | "audio/x-m4a" | "audio/mpeg4-generic" | "audio/mp4-generic" | "audio/MP4A-LATM" => SoundPlayerFormat.MP4_AAC
            case "audio/flac" | "audio/x-flac" => SoundPlayerFormat.NATIVE_FLAC
            case "audio/ogg" | "audio/vorbis" | "audio/x-ogg" | "application/ogg" | "application/x-ogg" | "audio/vorbis-config" => SoundPlayerFormat.OGG_VORBIS
            case "audio/mpeg" | "audio/mpeg3" | "audio/x-mpeg3" | "audio/mp3" | "audio/x-mpeg" | "audio/x-mp3" | "audio/mpg" | "audio/x-mpg" | "audio/x-mpegaudio" => SoundPlayerFormat.NATIVE_MP3
            case _ => throw new UnsupportedAudioFileException("Unsupported MIME type " + mime)
          }
        }
      case None => SoundPlayerFormat.UNKNOWN
    }
  }

  protected def checkHttpMimeType(urlConnection: URLConnection): SoundPlayerFormat = {
    checkMimeType(Option(urlConnection.getContentType))
  }

  private def fileExtension(filename: Option[String]): Option[String] = {
    filename match {
      case Some(f) => Option(f.substring(f.lastIndexOf('.') + 1, f.length))
      case None => None
    }
  }

  protected def checkFileExtension(fileName: Option[String]): SoundPlayerFormat = {
    val extension = fileExtension(fileName)
    logger.debug("file extension: " + extension)
    extension match {
      case Some(ext) => {
          ext.toLowerCase match {
            case "aac" => SoundPlayerFormat.RAW_AAC
              //ALAC is not supported with the standard MP4 mime type
            case "m4a" | "mp4" => SoundPlayerFormat.MP4_AAC
            case "flac" => SoundPlayerFormat.NATIVE_FLAC
            case "ogg" | "oga" => SoundPlayerFormat.OGG_VORBIS
            case "mp3" => SoundPlayerFormat.NATIVE_MP3
            case _ => throw new UnsupportedAudioFileException("Unsupported file extension " + ext)
          }
        }
      case None => throw new UnsupportedAudioFileException("No file extension ")
    }
  }

  def forcedAudioFileFormat(sourceStream: InputStream, format: SoundPlayerFormat): AudioFileFormat = { 
    format match {
      case SoundPlayerFormat.RAW_AAC => {
          Class.forName("net.sourceforge.jaad.spi.javasound.AACAudioFileReader")
          new net.sourceforge.jaad.spi.javasound.AACAudioFileReader().getAudioFileFormat(sourceStream)
        }
      case SoundPlayerFormat.MP4_AAC => {
          Class.forName("net.sourceforge.jaad.spi.javasound.MP4AudioFileReader")
          new net.sourceforge.jaad.spi.javasound.MP4AudioFileReader().getAudioFileFormat(sourceStream)
        }
      case SoundPlayerFormat.NATIVE_FLAC => {
          Class.forName("org.kc7bfi.jflac.sound.spi.FlacAudioFileReader")
          new org.kc7bfi.jflac.sound.spi.FlacAudioFileReader().getAudioFileFormat(sourceStream)
        }
      case SoundPlayerFormat.OGG_VORBIS => {
          Class.forName("javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader")
          new javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader().getAudioFileFormat(sourceStream)
        }
      case SoundPlayerFormat.NATIVE_MP3 => {
          Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader")
          new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader().getAudioFileFormat(sourceStream)
        }
      case _ =>
        throw new UnsupportedAudioFileException("Codec is not supported " + format)
    }
  }

  protected def forcedAudioInputStream(targetFormat: AudioFormat, sourceStream: AudioInputStream, format: SoundPlayerFormat): AudioInputStream = {
    format match {
      case ( SoundPlayerFormat.RAW_AAC | SoundPlayerFormat.MP4_AAC) =>
          throw new UnsupportedAudioFileException("AAC codec and derivatives are not supported");
      case SoundPlayerFormat.NATIVE_FLAC => {
          Class.forName("org.kc7bfi.jflac.sound.spi.FlacFormatConversionProvider")
          new org.kc7bfi.jflac.sound.spi.FlacFormatConversionProvider().getAudioInputStream(targetFormat, sourceStream)
        }
      case SoundPlayerFormat.OGG_VORBIS => {
          Class.forName("javazoom.spi.vorbis.sampled.convert.VorbisFormatConversionProvider")
          new javazoom.spi.vorbis.sampled.convert.VorbisFormatConversionProvider().getAudioInputStream(targetFormat, sourceStream)
        }
      case SoundPlayerFormat.NATIVE_MP3 => {
          Class.forName("javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider")
          new javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider().getAudioInputStream(targetFormat, sourceStream)
        }
      case _ =>
        throw new UnsupportedAudioFileException("Codec is not supported " + format)
    }
  }

  protected def forcedAudioInputStream(sourceStream: InputStream, format: SoundPlayerFormat): AudioInputStream = {
    format match {
      case SoundPlayerFormat.RAW_AAC => {
          Class.forName("net.sourceforge.jaad.spi.javasound.AACAudioFileReader")
          new net.sourceforge.jaad.spi.javasound.AACAudioFileReader().getAudioInputStream(sourceStream)
        }
      case SoundPlayerFormat.MP4_AAC => {
          Class.forName("net.sourceforge.jaad.spi.javasound.MP4AudioFileReader")
          new net.sourceforge.jaad.spi.javasound.MP4AudioFileReader().getAudioInputStream(sourceStream)
        }
      case SoundPlayerFormat.NATIVE_FLAC => {
          Class.forName("org.kc7bfi.jflac.sound.spi.FlacAudioFileReader")
          new org.kc7bfi.jflac.sound.spi.FlacAudioFileReader().getAudioInputStream(sourceStream)
        }
      case SoundPlayerFormat.OGG_VORBIS => {
          Class.forName("javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader")
          new javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader().getAudioInputStream(sourceStream)
        }
      case SoundPlayerFormat.NATIVE_MP3 => {
          Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFileReader")
          new javazoom.spi.mpeg.sampled.file.MpegAudioFileReader().getAudioInputStream(sourceStream)
        }
      case _ =>
        throw new UnsupportedAudioFileException("Codec is not supported " + format)
    }
  }

}