/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                               *
 *  Copyright  (C)  2015  Christian Krause                                                       *
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

object Summary extends Analysis with HasFileOpSummary {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- parseLogs) {
      val reads = entries collect { case entry: LogEntry.Read => entry }
      val writes = entries collect { case entry: LogEntry.Write => entry }

      val readSummary = reads.foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))
      val writeSummary = writes.foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))

      val readOutput = readSummary.humanized(op = "read")
      val writeOutput = writeSummary.humanized(op = "write")

      println(s"""$log $readOutput""")
      println(s"""$log $writeOutput""")
    }
}
