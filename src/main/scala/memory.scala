/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                               *
 *  Copyright  (C)  2015-2016  Christian Krause                                                  *
 *                                                                                               *
 *  Christian Krause  <christian.krause@mailbox.org>                                             *
 *                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                               *
 *  This file is part of strace-analyzer.                                                        *
 *                                                                                               *
 *  strace-analyzer is free software: you can redistribute it and/or modify it under the terms   *
 *  of the GNU General Public License as published by the Free Software Foundation, either       *
 *  version 3 of the License, or any later version.                                              *
 *                                                                                               *
 *  strace-analyzer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; *
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.    *
 *  See the GNU General Public License for more details.                                         *
 *                                                                                               *
 *  You should have received a copy of the GNU General Public License along with                 *
 *  strace-analyzer. If not, see <http://www.gnu.org/licenses/>.                                 *
 *                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


package strace
package analyze

object Memory extends Memory

trait Memory {
  val kibi = math.pow(2,10)
  val mebi = math.pow(2,20)
  val gibi = math.pow(2,30)
  val tebi = math.pow(2,40)
  val pebi = math.pow(2,50)
  val exbi = math.pow(2,60)
  val zebi = math.pow(2,70)

  object humanize {
    def apply(bytes: Long): String = bytes match {
      case _ if bytes.abs < kibi => f"""$bytes%d"""
      case _ if bytes.abs < mebi => f"""${(bytes/kibi).round}%dK"""
      case _ if bytes.abs < gibi => f"""${(bytes/mebi).round}%dM"""
      case _ if bytes.abs < tebi => f"""${(bytes/gibi).round}%dG"""
      case _ if bytes.abs < pebi => f"""${(bytes/tebi).round}%dT"""
      case _ if bytes.abs < exbi => f"""${(bytes/pebi).round}%dP"""
      case _ if bytes.abs < zebi => f"""${(bytes/exbi).round}%dE"""
      case _                     => f"""${(bytes/zebi).round}%dZ"""
    }
  }
}
