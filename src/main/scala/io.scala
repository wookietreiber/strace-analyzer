package strace
package analyze

trait PerFileSummary extends HasFileOpSummary {
  def readAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val reads = entries collect {
      case entry: LogEntry.Read => entry
    }

    val analysis = reads.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))
    }

    for ((file,analysis) <- analysis) {
      val output = analysis.humanized(op = "read")
      println(s"""$output $file""")
    }
  }

  def writeAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val writes = entries collect {
      case entry: LogEntry.Write => entry
    }

    val analysis = writes.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileOpSummary.empty)(_ + FileOpSummary(_))
    }

    for ((file,analysis) <- analysis) {
      val output = analysis.humanized(op = "write")
      println(s"""$output $file""")
    }
  }
}

object IO extends Analysis with PerFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      readAnalysis(entries)
      writeAnalysis(entries)
    }
}

object Read extends Analysis with PerFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      readAnalysis(entries)
    }
}

object Write extends Analysis with PerFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      writeAnalysis(entries)
    }
}
