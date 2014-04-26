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

import java.awt.Color

object HTMLColour {
  private lazy val colourNames = Map[String, Color](
    "black" -> new Color(0x000000),
    "green" -> new Color(0x008000),
    "silver" -> new Color(0xC0C0C0),
    "lime" -> new Color(0x00FF00),
    "gray" -> new Color(0x808080),
    "olive" -> new Color(0x808000),
    "white" -> new Color(0xFFFFFF),
    "yellow" -> new Color(0xFFFF00),
    "maroon" -> new Color(0x800000),
    "navy" -> new Color(0x000080),
    "red" -> new Color(0xFF0000),
    "blue" -> new Color(0x0000FF),
    "purple" -> new Color(0x800080),
    "teal" -> new Color(0x008080),
    "fuchsia" -> new Color(0xFF00FF),
    "aqua" -> new Color(0x00FFFF)
  )

  def decode(colourName: Option[String]): Option[Color] = {
    val preparedColourName =
      colourName
    .map(_.trim)
    .filter(_.length != 0)
    .get.toLowerCase
    if (colourNames.contains(preparedColourName))
      Some(colourNames(preparedColourName))
    else
      try {
        Option(Color.decode(preparedColourName))
      } catch {
        case e: NumberFormatException => /*throw new IllegalArgumentException("Unknown colour code given", e)*/ None
      }
  }

  def encode(colour: Option[Color]): Option[String] = {
    colour
    .map(_.getRGB)
    .map(Integer.toHexString(_))
    .map("#" + _.substring(2).toUpperCase)
  }

  def integerValue(colourName: Option[String]): Option[Int] = {
    decode(colourName)
    .map(_.getRGB)
  }

  /** Unit test */
  /*def main(args: Array[String]) {
   println( "dummy colour:" +
   integerValue(Option("dummy"))
   .map(Integer.toHexString(_))
   )
   println( "blue colour:" +
   integerValue(Option("blue"))
   .map(Integer.toHexString(_))
   )
   }*/

}
