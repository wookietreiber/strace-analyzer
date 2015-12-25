package strace
package analyze

trait Analysis {
  def analyze(implicit config: Config): Unit

  // TODO scalaz-stream / fs2
  // TODO scopt apparently incapable of handling - as a file
  def filesToEntries(implicit config: Config): Map[String,List[LogEntry]] = {
    if (config.logs.isEmpty)
      Map("STDIN" -> getEntries(io.Source.stdin))
    else {
      val xs = for {
        log <- config.logs.distinct
        source = io.Source.fromFile(log)
        entries = getEntries(source)
      } yield log.getName -> entries

      xs.toMap
    }
  }

  def getEntries(log: io.Source): List[LogEntry] = try {
    val fdDB = collection.mutable.Map[String,String]()

    log.getLines.collect({
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
  } finally {
    log.close()
  }
}

trait HasFileSummary {
  def readAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val reads = entries collect {
      case entry: LogEntry.Read => entry
    }

    val readAnalysis = reads.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileSummary.empty("read"))(_ + FileSummary(_))
    }

    readAnalysis foreach {
      case (file,analysis)
          if config.regex.map(_.findFirstIn(file).isDefined).orElse(config.filter.map(file.contains)).getOrElse(true) =>
        println(analysis.msg(file))
      case _ =>
    }
  }

  def writeAnalysis(entries: List[LogEntry])(implicit config: Config): Unit = {
    val writes = entries collect {
      case entry: LogEntry.Write => entry
    }

    val writeAnalysis = writes.groupBy(_.fd) mapValues { entries =>
      entries.foldLeft(FileSummary.empty("write"))(_ + FileSummary(_))
    }

    writeAnalysis foreach {
      case (file,analysis)
          if config.regex.map(_.findFirstIn(file).isDefined).orElse(config.filter.map(file.contains)).getOrElse(true) =>
        println(analysis.msg(file))
      case _ =>
    }
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

    def msg(file: String) = s"""$op $hBytes in $hSeconds (~ $hbps / s) with $ops ops (~ $hbpo / o) $file"""
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
    for ((log,entries) <- filesToEntries) {
      readAnalysis(entries)
      writeAnalysis(entries)
    }
}

object Read extends Analysis with HasFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- filesToEntries) {
      readAnalysis(entries)
    }
}

object Write extends Analysis with HasFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- filesToEntries) {
      writeAnalysis(entries)
    }
}
