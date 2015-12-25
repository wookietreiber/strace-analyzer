package strace
package analyze

trait HasFileSummary {
  def readAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val reads = entries collect {
      case entry: LogEntry.Read => entry
    }

    val analysis = reads.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileSummary.empty)(_ + FileSummary(_))
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
      entries.foldLeft(FileSummary.empty)(_ + FileSummary(_))
    }

    for ((file,analysis) <- analysis) {
      val output = analysis.humanized(op = "write")
      println(s"""$output $file""")
    }
  }

  case class FileSummary(bytes: Long, ops: Long, seconds: Double) {
    def +(that: FileSummary): FileSummary = FileSummary (
      bytes = this.bytes + that.bytes,
      ops = this.ops + that.ops,
      seconds = this.seconds + that.seconds
    )

    def bps = bytes / seconds
    def bpo = bytes.toDouble / ops
    def hBytes = Memory.humanize(bytes)
    def hSeconds = Duration.humanize(seconds)
    def hbps = Memory.humanize(bps.round)
    def hbpo = Memory.humanize(bpo.round)

    def humanized(op: String) =
      s"""$op $hBytes in $hSeconds (~ $hbps / s) with $ops ops (~ $hbpo / op)"""
  }

  object FileSummary {
    val empty = FileSummary(bytes = 0L, ops = 0L, seconds = 0.0)

    def apply(read: LogEntry.Read): FileSummary =
      FileSummary(bytes = read.bytes, ops = 1, seconds = read.time.toDouble)

    def apply(write: LogEntry.Write): FileSummary =
      FileSummary(bytes = write.bytes, ops = 1, seconds = write.time.toDouble)
  }
}

object IO extends Analysis with HasFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      readAnalysis(entries)
      writeAnalysis(entries)
    }
}

object Read extends Analysis with HasFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      readAnalysis(entries)
    }
}

object Write extends Analysis with HasFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((_,entries) <- parseLogs) {
      writeAnalysis(entries)
    }
}
