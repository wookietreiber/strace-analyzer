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

trait PerFileSummary extends HasFileOpSummary {
  def readAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val reads = entries collect {
      case entry: LogEntry.Read => entry
    }

    val analysis = reads.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))
    }

    for ((file,analysis) <- analysis) {
      val output = analysis.humanized(op = "read")
      println(s"""$output $file""")
    }
  }

  def writeAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val writes = entries collect {
      case entry: LogEntry.Write => entry
    }

    val analysis = writes.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))
    }

    for ((file,analysis) <- analysis) {
      val output = analysis.humanized(op = "write")
      println(s"""$output $file""")
    }
  }
}

object IO extends Analysis with PerFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      readAnalysis(entries)
      writeAnalysis(entries)
    }
}

object Read extends Analysis with PerFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      readAnalysis(entries)
    }
}

object Write extends Analysis with PerFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      writeAnalysis(entries)
    }
}
