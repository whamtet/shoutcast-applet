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

import io.Source
import java.net.{ URISyntaxException, URI }

import biz.ddcr.shampoo.applet.misc.URIFixer._

class PLSStreamingEndpoint(private val uri: URI, private val charset: Option[String]) extends StreamingEndpoint(uri, charset) {

  def extensionOK(extension: Option[String]) = extension.isDefined && extension.get.equalsIgnoreCase("pls")

  def mimeTypeOk(mime: Option[String]) = mime.isDefined && (mime.get.equalsIgnoreCase("audio/x-scpls") || mime.get.equalsIgnoreCase("audio/scpls"))

  override def readBuffer(in: Source): Option[Iterable[URI]] = {

    Option(in
           .getLines
           .map(_.trim)
           .filter(l => !l.isEmpty() && l.regionMatches(true, 0, "file", 0, 4))
           //using flatmap instead of map in order to avoid having to deal with Option[URI] which is useless here
           .flatMap(
        l => {
          try {
            List(resolveFileURIIfAny(new URI(l.substring(l.indexOf('=', 4)+1))))
          } catch {
            case e @ (_: URISyntaxException | _: IndexOutOfBoundsException) => {
                logger.error("Invalid playlist entry: " + l, e)
                Nil
              }
          }
        })
           .toList)

  }

}
