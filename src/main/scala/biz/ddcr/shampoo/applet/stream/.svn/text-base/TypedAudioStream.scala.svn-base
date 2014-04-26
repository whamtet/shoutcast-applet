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

import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream

object SoundPlayerFormat extends Enumeration {
  type SoundPlayerFormat = Value
  val UNKNOWN, RAW_AAC, NATIVE_MP3, NATIVE_FLAC, OGG_VORBIS, MP4_AAC = Value
}

import SoundPlayerFormat._

class TypedAudioStream(private val _guessedAudioCodec: SoundPlayerFormat, private val _audioInputStream: AudioInputStream, private val _audioFileFormat: Option[AudioFileFormat]) extends Serializable {

  def guessedAudioCodec: SoundPlayerFormat = _guessedAudioCodec
  def audioInputStream: AudioInputStream = _audioInputStream
  def audioFileFormat: Option[AudioFileFormat] = _audioFileFormat

}