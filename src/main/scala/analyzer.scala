package strace
package analyze

import java.io.File
import scala.util.matching.Regex

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

    cmd("io") text("The io command compiles a read/write operation summary.") action { (_, c) =>
      c.copy(command = "io")
    } children (
      opt[String]("filter") action { (x, c) =>
        c.copy(filter = Some(x))
      } text("filenames/paths must contain this string (exact match)") valueName("path"),

      opt[String]("regex") action { (x, c) =>
        c.copy(regex = Some(x.r))
      } text("filenames/paths must match in part this regex (regex has more weight than exact match)") valueName("regex"),

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
