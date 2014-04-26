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

import java.io.File
import java.net.URI

object URIFixer {
  def resolveFileURIIfAny(uri: URI): URI = {
    //Windows drive letters can be mistaken for schemes so handle this limit case
    val windowsLetterDrive = """^([a-zA-Z])$""".r
    Option(uri.getScheme).getOrElse("file").toLowerCase match {
      case "file" => (new File(uri.getPath)).getCanonicalFile().toURI
      case windowsLetterDrive(_) => (new File(uri.toString)).getCanonicalFile().toURI
      case _ => uri
    }
  }
}
