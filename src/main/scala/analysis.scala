package strace
package analyze

/**
  * @todo scalaz-stream / fs2
  */
abstract class Analysis {

  /** Analyzes strace logs and prints the result to STDOUT. */
  def analyze(implicit config: Config): Unit

  /** Returns the log entries grouped by log input. */
  def parseLogs(implicit config: Config): Map[String,List[LogEntry]] =
    if (config.logs.isEmpty)
      Map("STDIN" -> parseLog(io.Source.stdin))
    else {
      val xs = for {
        log <- config.logs.distinct
        source = io.Source.fromFile(log)
        entries = parseLog(source)
      } yield log.getName -> entries

      xs.toMap
    }

  /** Returns a parsed strace log. */
  def parseLog(log: io.Source): List[LogEntry] = try {
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
