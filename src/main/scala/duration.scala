package strace
package analyze

object Duration extends Duration

trait Duration {

  object humanize {
    def apply(seconds: Double): String = seconds match {
      case _ if seconds > 1.0   => f"""$seconds%1.3f s"""
      case _ if seconds > 0.001 => f"""${(seconds * 1000000L).round.toDouble / 1000L}%1.3f ms"""
      case _                    => f"""${(seconds * 1000000L).round}%d us"""
    }
  }
}
