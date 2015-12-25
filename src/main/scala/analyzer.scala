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
