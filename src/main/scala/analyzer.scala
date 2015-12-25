package strace
package analyze

import java.io.File

object Analyzer extends App {

  def noteText = s"""|Generate logs like this:
                     |
                     |  strace -ff -T -ttt -o app-strace.log app
                     |
                     |Other formats are not supported.
                     |""".stripMargin

  val parser = new scopt.OptionParser[Config](BuildInfo.name) {
    head(BuildInfo.name, BuildInfo.version)

    note(noteText)

    help("help") abbr("h") text("prints this usage text")

    version("version") text("prints version number")

    def logs = arg[File]("<log1> <log2> ...") optional() unbounded() text (
      "strace log files, reads from STDIN if none are given"
    ) action { (x, c) =>
      c.copy(logs = x :: c.logs)
    } validate { file =>
      if (file.exists) success else failure(s"$file does not exist.")
    }

    cmd("io") text("The io command compiles a read/write operation summary.") action { (_, c) =>
      c.copy(command = Some(IO))
    } children ( logs )

    cmd("read") text("The read command compiles a read operation summary.") action { (_, c) =>
      c.copy(command = Some(Read))
    } children ( logs )

    cmd("write") text("The write command compiles a write operation summary.") action { (_, c) =>
      c.copy(command = Some(Write))
    } children ( logs )

    checkConfig { config =>
      if (config.command.isEmpty)
        failure("No command given.")
      else
        success
    }
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      config.command.get.analyze(config)

    case None =>
      sys exit 1
  }
}
