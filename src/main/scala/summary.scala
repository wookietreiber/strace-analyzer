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

object Summary extends Analysis with HasFileOpSummary {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- parseLogs) {
      printSummary(log, entries, "read") {
        case entry: LogEntry.PRead => entry
        case entry: LogEntry.Read => entry
      }

      printSummary(log, entries, "write") {
        case entry: LogEntry.PWrite => entry
        case entry: LogEntry.Write => entry
      }
    }

  def printSummary(log: String, entries: List[LogEntry], op: String)
    (pf: PartialFunction[LogEntry,LogEntry with HasBytes]): Unit = {

    val summary = entries.collect(pf).foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))

    val output = summary.humanized(op)

    println(s"""$log $output""")
  }
}
