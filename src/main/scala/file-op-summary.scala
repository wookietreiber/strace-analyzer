package strace
package analyze

trait HasFileOpSummary {
  case class FileOpSummary(bytes: Long, ops: Long, seconds: Double) {
    def +(that: FileOpSummary): FileOpSummary = FileOpSummary (
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

  object FileOpSummary {
    val empty = FileOpSummary(bytes = 0L, ops = 0L, seconds = 0.0)

    def apply(read: LogEntry.Read): FileOpSummary =
      FileOpSummary(bytes = read.bytes, ops = 1, seconds = read.time.toDouble)

    def apply(write: LogEntry.Write): FileOpSummary =
      FileOpSummary(bytes = write.bytes, ops = 1, seconds = write.time.toDouble)
  }
}
