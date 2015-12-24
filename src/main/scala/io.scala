package strace
package analyze

import java.io.File

class IO(config: Config) {

  // TODO if "-" read from STDIN
  // TODO scalaz-stream / fs2

  if (config.logs.isEmpty)
    handleLog(io.Source.stdin)
  else
    config.logs foreach { logFile =>
      val source = io.Source.fromFile(logFile)
      try {
        handleLog(source)
      } finally {
        source.close()
      }
    }

  def handleLog(log: io.Source): Unit = {

    val fdDB = collection.mutable.Map[String,String]()

    val entries: List[LogEntry] = log.getLines.collect({
      case LogEntry.Close(close) if close.status >= 0 =>
        fdDB -= close.fd
        close

      case LogEntry.Creat(creat) if creat.status >= 0 =>
        fdDB += (creat.fd -> creat.file)
        creat

      case LogEntry.Dup(dup) if dup.status >= 0 =>
        val where = fdDB get dup.oldFd
        val file = where.fold("no entry for dup found, probably PIPE")(identity)
        fdDB += (dup.newFd -> file)
        dup

      case LogEntry.Open(open) if open.status >= 0 =>
        fdDB += (open.fd -> open.file)
        open

      case LogEntry.OpenAt(openat) if openat.status >= 0 =>
        val where = fdDB get openat.wherefd
        val file = where.fold(openat.filename)(openat.file)
        fdDB += (openat.fd -> file)
        openat

      case LogEntry.Pipe(pipe) if pipe.status >= 0 =>
        fdDB += (pipe.read -> "PIPE")
        fdDB += (pipe.write -> "PIPE")
        pipe

      case LogEntry.Read(read) if read.status >= 0 =>
        fdDB.get(read.fd).fold(read)(file => read.copy(fd = file))

      case LogEntry.Write(write) if write.status >= 0 =>
        fdDB.get(write.fd).fold(write)(file => write.copy(fd = file))
    }).toList

    log.close()

    val reads = entries collect {
      case entry: LogEntry.Read => entry
    }

    val readAnalysis = reads.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(ReadAnalysis.empty)(_ + ReadAnalysis(_))
    }

    readAnalysis.toSeq sortBy { _._2.bps } foreach {
      case (file,analysis) =>
        println(s"""$file ${analysis.msg}""")
    }

    val writes = entries collect {
      case entry: LogEntry.Write => entry
    }

    val writeAnalysis = writes.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(WriteAnalysis.empty)(_ + WriteAnalysis(_))
    }

    writeAnalysis.toSeq sortBy { _._2.bps } foreach {
      case (file,analysis) =>
        println(s"""$file ${analysis.msg}""")
    }
  }

  case class ReadAnalysis(bytes: Long, seconds: Double) {
    def +(that: ReadAnalysis): ReadAnalysis = ReadAnalysis (
      bytes = this.bytes + that.bytes,
      seconds = this.seconds + that.seconds
    )

    def bps = bytes / seconds

    def msg = s"""read $bytes bytes in $seconds seconds"""
  }

  object ReadAnalysis {
    val empty = ReadAnalysis(0L, 0.0)

    def apply(read: LogEntry.Read): ReadAnalysis = ReadAnalysis (
      bytes = read.bytes,
      seconds = read.time.toDouble
    )
  }

  case class WriteAnalysis(bytes: Long, seconds: Double) {
    def +(that: WriteAnalysis): WriteAnalysis = WriteAnalysis (
      bytes = this.bytes + that.bytes,
      seconds = this.seconds + that.seconds
    )

    def bps = bytes / seconds

    def msg = s"""wrote $bytes bytes in $seconds seconds"""
  }

  object WriteAnalysis {
    val empty = WriteAnalysis(0L, 0.0)

    def apply(write: LogEntry.Write): WriteAnalysis = WriteAnalysis (
      bytes = write.bytes,
      seconds = write.time.toDouble
    )
  }

}
