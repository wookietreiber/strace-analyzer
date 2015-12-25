package strace
package analyze

object Summary extends Analysis with HasFileSummary {
  def analyze(implicit config: Config): Unit =
    for ((log,entries) <- parseLogs) {
      val reads = entries collect { case entry: LogEntry.Read => entry }
      val writes = entries collect { case entry: LogEntry.Write => entry }

      val readSummary = reads.foldLeft(FileSummary.empty)(_ + FileSummary(_))
      val writeSummary = writes.foldLeft(FileSummary.empty)(_ + FileSummary(_))

      val readOutput = readSummary.humanized(op = "read")
      val writeOutput = writeSummary.humanized(op = "write")

      println(s"""$log $readOutput""")
      println(s"""$log $writeOutput""")
    }
}
