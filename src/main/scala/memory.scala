package strace
package analyze

object Memory extends Memory

trait Memory {
  val kibi = math.pow(2,10)
  val mebi = math.pow(2,20)
  val gibi = math.pow(2,30)
  val tebi = math.pow(2,40)
  val pebi = math.pow(2,50)
  val exbi = math.pow(2,60)
  val zebi = math.pow(2,70)

  object humanize {
    def apply(bytes: Long): String = bytes match {
      case _ if bytes.abs < kibi => f"""$bytes%d"""
      case _ if bytes.abs < mebi => f"""${(bytes/kibi).round}%dK"""
      case _ if bytes.abs < gibi => f"""${(bytes/mebi).round}%dM"""
      case _ if bytes.abs < tebi => f"""${(bytes/gibi).round}%dG"""
      case _ if bytes.abs < pebi => f"""${(bytes/tebi).round}%dT"""
      case _ if bytes.abs < exbi => f"""${(bytes/pebi).round}%dP"""
      case _ if bytes.abs < zebi => f"""${(bytes/exbi).round}%dE"""
      case _                     => f"""${(bytes/zebi).round}%dZ"""
    }
  }
}
