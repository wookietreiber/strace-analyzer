package strace
package analyze

import java.io.File

case class Config(command: Analysis = Summary, logs: List[File] = Nil)
