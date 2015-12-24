package strace

import java.io.File

object Main extends App {

  case class Config(command: String = "", logs: List[File] = Nil)

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

    cmd("io") text("The io command compiles a read/write operation summary.") action { (_, c) =>
      c.copy(command = "io")
    } children (
      arg[File]("<log1> <log2> ...") optional() unbounded() text (
        "strace log files, reads from STDIN if none are given"
      ) action { (x, c) =>
        c.copy(logs = x :: c.logs)
      } validate { file =>
        if (file.exists) success else failure(s"$file does not exist.")
      }
    )

    checkConfig { config =>
      if (config.command.isEmpty)
        failure("No command given.")
      else
        success
    }
  }

  parser.parse(args, Config()) match {
    case Some(config) if config.command == "io" =>
      new IO(config)

    case None =>
      sys exit 1
  }
}
