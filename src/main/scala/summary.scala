package strace
package analyze

object Summary extends Analysis {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- parseLogs) {
      val reads = entries collect {
        case entry: LogEntry.Read => entry
      }

      val writes = entries collect {
        case entry: LogEntry.Write => entry
      }

      val readSummary = reads.foldLeft(ReadSummary.empty)(_ + ReadSummary(_))
      val writeSummary = writes.foldLeft(WriteSummary.empty)(_ + WriteSummary(_))

      println(readSummary.msg(log))
      println(writeSummary.msg(log))
    }

  case class ReadSummary(ops: Long, bytes: Long, seconds: Double) {
    def +(that: ReadSummary) = ReadSummary (
      this.ops + that.ops,
      this.bytes + that.bytes,
      this.seconds + that.seconds
    )

    def bps = bytes / seconds
    def bpo = bytes.toDouble / ops
    def hBytes = Memory.humanize(bytes)
    def hSeconds = Duration.humanize(seconds)
    def hbps = Memory.humanize(bps.round)
    def hbpo = Memory.humanize(bpo.round)

    def msg(log: String) = s"""$log read $hBytes in $hSeconds (~ $hbps / s) with $ops ops (~ $hbpo / op)"""
  }

  object ReadSummary {
    val empty = ReadSummary(0L, 0L, 0.0)
    def apply(read: LogEntry.Read): ReadSummary =
      ReadSummary(ops = 1, bytes = read.bytes, seconds = read.time.toDouble)
  }

  case class WriteSummary(ops: Long, bytes: Long, seconds: Double) {
    def +(that: WriteSummary) = WriteSummary (
      this.ops + that.ops,
      this.bytes + that.bytes,
      this.seconds + that.seconds
    )

    def bps = bytes / seconds
    def bpo = bytes.toDouble / ops
    def hBytes = Memory.humanize(bytes)
    def hSeconds = Duration.humanize(seconds)
    def hbps = Memory.humanize(bps.round)
    def hbpo = Memory.humanize(bpo.round)

    def msg(log: String) = s"""$log write $hBytes in $hSeconds (~ $hbps / s) with $ops ops (~ $hbpo / op)"""
  }

  object WriteSummary {
    val empty = WriteSummary(0L, 0L, 0.0)
    def apply(write: LogEntry.Write): WriteSummary =
      WriteSummary(ops = 1, bytes = write.bytes, seconds = write.time.toDouble)
  }
}
