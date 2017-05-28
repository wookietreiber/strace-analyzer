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

/**
  * @todo scopt apparently incapable of handling - as a file
  */
object Analyzer extends App {

  def noteText = s"""|Generate logs like this:
                     |
                     |  strace -ff -T -ttt -o app-strace.log app
                     |
                     |Other formats are not supported.
                     |
                     |If no command is given, summary will be used as the default.
                     |""".stripMargin

  val parser = new scopt.OptionParser[Config](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    note(noteText)

    help("help") abbr("h") text("prints this usage text")

    version("version") text("prints version number")

    arg[File]("<log1> <log2> ...") optional() unbounded() text (
      "strace log files, reads from STDIN if none are given"
    ) action { (x, c) =>
      c.copy(logs = x :: c.logs)
    } validate { file =>
      if (file.exists) success else failure(s"$file does not exist.")
    }

    cmd("io") text("The io command compiles a read/write operation summary.") action { (_, c) =>
      c.copy(command = IO)
    }

    cmd("read") text("The read command compiles a read operation summary.") action { (_, c) =>
      c.copy(command = Read)
    }

    cmd("summary") text("The summary command compiles a short per operation summary.") action { (_, c) =>
      c.copy(command = Summary)
    }

    cmd("write") text("The write command compiles a write operation summary.") action { (_, c) =>
      c.copy(command = Write)
    }
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      config.command.analyze(config)

    case None =>
      sys exit 1
  }
}
