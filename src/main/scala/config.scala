package strace
package analyze

import java.io.File
import scala.util.matching.Regex

case class Config(
  command: String = "",
  logs: List[File] = Nil,
  filter: Option[String] = None,
  regex: Option[Regex] = None
)
