package strace
package analyze

import java.io.File

case class Config(command: String = "", logs: List[File] = Nil)
