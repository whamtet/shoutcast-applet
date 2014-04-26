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
import java.net.URI

import biz.ddcr.shampoo.applet.misc.URIFixer._

class RawStreamingEndpoint(private val uri: URI, private val charset: Option[String]) extends StreamingEndpoint(uri, charset) {

  def extensionOK(extension: Option[String]) = throw new UnsupportedOperationException("Not supported yet.")

  def mimeTypeOk(mime: Option[String]) = throw new UnsupportedOperationException("Not supported yet.")

  def readBuffer(in: Source) = {
    throw new UnsupportedOperationException("Not supported yet.")
  }
  
  override def readPlaylist(uri: URI, charset: Option[String]): Iterable[URI] = {
    //create a list with a single entry
    resolveFileURIIfAny(uri) :: Nil
  }

}
