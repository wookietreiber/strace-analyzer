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

import java.io.File

object Analyzer extends App {

  // -----------------------------------------------------------------------------------------------
  // help / usage / version
  // -----------------------------------------------------------------------------------------------

  if (List("-version", "--version") exists args.contains) {
    Console.println(s"""${BuildInfo.name} ${BuildInfo.version}""")
    sys exit 0
  }

  if (List("-?", "-h", "-help", "--help") exists args.contains) {
    Console.println(s"""
      |${BuildInfo.name} ${BuildInfo.version}
      |
      |Usage: strace-analyzer [io|read|summary|write] [<log1> <log2> ...]
      |
      |Generate logs like this:
      |
      |  strace -ff -T -ttt -o app-strace.log app
      |
      |Other formats are not supported.
      |
      |  -? | -h | -help | --help            print this help
      |  -version | --version                print version
      |
      |Command: io
      |  The io command compiles a read/write operation summary.
      |
      |Command: read
      |  The read command compiles a read operation summary.
      |
      |Command: summary
      |  The summary command compiles a short per operation summary.
      |
      |Command: write
      |  The write command compiles a write operation summary.
      |""".stripMargin)
    sys exit 0
  }

  // -----------------------------------------------------------------------------------------------
  // parse cli args
  // -----------------------------------------------------------------------------------------------

  val command = args.headOption match {
    case None =>
      Console.err.println("error: need command")
      sys exit 1

    case Some("io")      => IO
    case Some("read")    => Read
    case Some("summary") => Summary
    case Some("write")   => Write

    case Some(other) =>
      Console.err.println(s"""error: don't know the command "$other"""")
      sys exit 1
  }

  def accumulate(conf: Config)(args: List[String]): Config = args match {
    case Nil =>
      conf.copy(logs = conf.logs.reverse)

    case x :: tail =>
      val file = new File(x)
      if (!file.exists) {
        Console.err.println(s"""error: file "$x" does not exist""")
        sys exit 1
      }
      accumulate(conf.copy(logs = file :: conf.logs))(tail)
  }

  val conf = accumulate(Config())(args.toList.tail)

  // -----------------------------------------------------------------------------------------------
  // analyze
  // -----------------------------------------------------------------------------------------------

  command.analyze(conf)

}
