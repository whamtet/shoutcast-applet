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

package biz.ddcr.shampoo.applet.misc

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import java.io.InputStream
import scala.io.Source

class HTTPHeader(private val inputStream: InputStream) {

  private val logger: Log = LogFactory.getLog(this.getClass)

  private lazy val headers: Map[String, String] = parseHeaders(inputStream)

  private def parseHeaders(inputStream: InputStream): Map[String, String] = {
    /** Get the full HTTP header until the \r\n\r\n */
    val lines = Source.fromInputStream(inputStream).getLines.takeWhile(_ != "")
    lines.foldLeft(Map.empty[String, String]) {
      case (m, l) => {
          //Split the line into components if it's a real header    		
          //LWS-char (see HTTP/1.0 or HTTP/1.1 Section 2.2) is NOT supported (yet)!
          val keyValue = l.split(":").map(_.trim.toLowerCase)
          if (keyValue.size > 1) {
            m.updated(keyValue(0), keyValue(1))
          } else {
            //Store the HTTP response as a fake header as well
            if (keyValue.startsWith("HTTP/")) {
              val httpResponse = l.split("/").map(_.trim.toLowerCase)
              if (httpResponse.size > 1) {
                m.updated("http-response", httpResponse(1))
              } else m
            } else m
          }
        }
    }
  }

  def httpResponse: Option[String] = {
    entry("http-response")
  }
  
  def entry(key: String): Option[String] = {
    headers.get(key)
  }
  
}