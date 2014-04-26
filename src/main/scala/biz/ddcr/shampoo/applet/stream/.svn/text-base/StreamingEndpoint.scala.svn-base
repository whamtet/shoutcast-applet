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
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection

import scala.io.Source

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import biz.ddcr.shampoo.applet.misc.URIFixer._

abstract class StreamingEndpoint(private val uri: URI, private val charset: Option[String]) extends Iterator[URI] {

  protected val logger: Log = LogFactory.getLog(this.getClass)

  protected val items: Iterable[URI] = readPlaylist(uri, charset)
  protected var itemsIterator: Option[Iterator[URI]] = None

  def mimeTypeOk(mime: Option[String]): Boolean

  def extensionOK(extension: Option[String]): Boolean

  def readBuffer(in: Source): Option[Iterable[URI]]

  private def iterator: Iterator[URI] = {
    itemsIterator match {
      case None => {
          //Reinstantiate a new iterator: this is how it's rewinded
          itemsIterator = Some(items.iterator)
          itemsIterator.get
        }
      case Some(itemsIterator) => itemsIterator
    }
  }

  def hasNext = {
    iterator.hasNext
  }

  def next = {
    iterator.next
  }

  def rewind: Unit = {
    itemsIterator = None
  }

  def readPlaylist(uri: URI, charset: Option[String]): Iterable[URI] = {
    val fixedURI = resolveFileURIIfAny(uri)
    fixedURI.getScheme.toLowerCase match {
      case "file" => readPlaylist(new File(fixedURI), charset)
      case _ => readPlaylist(fixedURI.toURL, charset)
    }
  }
  
  def readPlaylist(file: File, charset: Option[String]): Iterable[URI] = {
    val fileName: String = file.getName
    val extension: Option[String] = Option(fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length))
    if (extensionOK(extension)) {
      
      val in: Source = Source.fromFile(file)
      val localItems: Option[Iterable[URI]] = readBuffer(in)
      try {
        if (!localItems.isDefined || localItems.get.isEmpty) {
          throw new IllegalStateException("Empty endpoint")
        }
      } finally {
        in.close
      }
      localItems.get
    } else
      throw new UnsupportedOperationException("Unknown or unsupported streaming endpoint format")
  }
  
  def readPlaylist(url: URL, charset: Option[String]): Iterable[URI] = {

    //Check Mime type
    //Don't load the whole stream if possible, use HEAD for example with HTTP-based connections
    //Otherwise the connection will hang until the content is fully loaded and this can take too much time if the URL is not targeted toward a playlist but a huge binary audio file
    //TODO: No workaround yet for other kinds of URLConnections but it should not be a major problem since only HTTP and JAR protocols are implemented in JDKs up to 7
    var urlConnection: Option[URLConnection] = Option(CodecSPIWorkaround.keepAliveReadOptions(url.openConnection))
    urlConnection match {
      case Some(u: HttpURLConnection) => u.setRequestMethod("HEAD")
      case None => new IllegalStateException("Invalid endpoint")
      case _ => Unit
    }
    val fileType: Option[String] = Option(urlConnection.get.getContentType)
    urlConnection match {
      case Some(u: HttpURLConnection) => {
          u.disconnect
          urlConnection = None
        }
      case None => new IllegalStateException("Invalid endpoint")
      case _ => Unit
    }
    //And then the extension if it fails
    val fileName: String = uri.getPath
    val extension: Option[String] = Option(fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length))
    if (mimeTypeOk(fileType) || extensionOK(extension)) {
      if (!urlConnection.isDefined) urlConnection = Option(CodecSPIWorkaround.defaultReadOptions(url.openConnection))
      val in: Source = charset match {
        case None => Source.fromInputStream(urlConnection.get.getInputStream)
        case Some(charset) => Source.fromInputStream(urlConnection.get.getInputStream, charset)
      }
      val localItems: Option[Iterable[URI]] = readBuffer(in)
      try {
        if (!localItems.isDefined || localItems.get.isEmpty) {
          throw new IllegalStateException("Empty endpoint")
        }
      } finally {
        in.close
      }
      localItems.get
    } else
      throw new UnsupportedOperationException("Unknown or unsupported streaming endpoint format")
  }

}

object StreamingEndpoint {
  def parse(uri: URI, charset: String = "UTF-8"): StreamingEndpoint = {
    try {
      new M3UStreamingEndpoint(uri, Option(charset))
    } catch {
      case e: UnsupportedOperationException => {
          try {
            new PLSStreamingEndpoint(uri, Option(charset))
          } catch {
            case e: UnsupportedOperationException => {
                new RawStreamingEndpoint(uri, Option(charset))
              }
          }
        }
    }

  }
}
