package strace
package analyze

trait HasFileSummary {
  def readAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val reads = entries collect {
      case entry: LogEntry.Read => entry
    }

    val analysis = reads.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileSummary.empty("read"))(_ + FileSummary(_))
    }

    for ((file,analysis) <- analysis)
      println(analysis.msg(file))
  }

  def writeAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val writes = entries collect {
      case entry: LogEntry.Write => entry
    }

    val analysis = writes.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileSummary.empty("write"))(_ + FileSummary(_))
    }

    for ((file,analysis) <- analysis)
      println(analysis.msg(file))
  }

  case class FileSummary(op: String, bytes: Long, ops: Long, seconds: Double) {
    def +(that: FileSummary): FileSummary = FileSummary (
      op = this.op,
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

    def msg(file: String) = s"""$op $hBytes in $hSeconds (~ $hbps / s) with $ops ops (~ $hbpo / op) $file"""
  }

  object FileSummary {
    def empty(op: String) = FileSummary(op, bytes = 0L, ops = 0L, seconds = 0.0)

    def apply(read: LogEntry.Read): FileSummary =
      FileSummary(op = "read", bytes = read.bytes, ops = 1, seconds = read.time.toDouble)

    def apply(write: LogEntry.Write): FileSummary =
      FileSummary(op = "write", bytes = write.bytes, ops = 1, seconds = write.time.toDouble)
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
