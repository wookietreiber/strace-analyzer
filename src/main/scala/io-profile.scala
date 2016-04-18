/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                               *
 *  Copyright  (C)  2015-2016  Christian Krause                                                  *
 *                                                                                               *
 *  Christian Krause  <kizkizzbangbang@gmail.com>                                                *
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

import scalax.chart.api._

object IOProfile extends Analysis {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- parseLogs) {
      saveChart(log, entries, op = "read") {
        case entry: LogEntry.Read => entry
      }

      saveChart(log, entries, op = "write") {
        case entry: LogEntry.Write => entry
      }
    }

  def saveChart(log: String, entries: List[LogEntry], op: String)
    (pf: PartialFunction[LogEntry,LogEntry with HasBytes with HasFD]): Unit = {
    val filtered = entries.collect(pf)

    for ((file,entries) <- filtered.groupBy(_.fd)) {
      val filename = new java.io.File(file).getName
      val logname = new java.io.File(log).getName

      val chart = genChart(entries)

      chart.saveAsPNG (
        file = s"""strace-analyzer-profile-$op-$logname-$filename.png""",
        resolution = (1920,1080)
      )
    }
  }

  def genChart[A <: LogEntry with HasBytes](entries: List[A]) = {
    import java.text._
    import java.util.Date
    import org.jfree.chart.axis.NumberAxis
    import org.jfree.data.time.Second

    val raw = for {
      entry <- entries
      time = new Second(new Date(entry.jepoch))
      value = entry.bytes
    } yield (time,value)

    val data = raw.groupBy(_._1).mapValues(_.foldLeft(0L)(_ + _._2))

    val chart = XYBarChart(data.toTimeSeries(""), legend = false)
    chart.plot.range.axis.label.text = "bytes"
    chart.plot.range.axis.peer match {
      case axis: NumberAxis =>
        axis setNumberFormatOverride new NumberFormat {
          def format(value: Long, buf: StringBuffer, fp: FieldPosition): StringBuffer =
            buf append Memory.humanize(value)
          def format(value: Double, buf: StringBuffer, fp: FieldPosition): StringBuffer =
            format(value.round, buf, fp)
          def parse(value: String, pp: ParsePosition): Number = ???
        }

      case _ =>
    }

    chart
  }
}
