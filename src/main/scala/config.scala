package strace
package analyze

import java.io.File

case class Config(command: Option[Analysis] = None, logs: List[File] = Nil)
