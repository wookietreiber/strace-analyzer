/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                               *
 *  Copyright  (C)  2015-2016  Christian Krause                                                  *
 *                                                                                               *
 *  Christian Krause  <christian.krause@mailbox.org>                                             *
 *                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                                               *
 *  This file is part of strace-analyzer.                                                        *
 *                                                                                               *
 *  strace-analyzer is free software: you can redistribute it and/or modify it under the terms   *
 *  of the GNU General Public License as published by the Free Software Foundation, either       *
 *  version 3 of the License, or any later version.                                              *
 *                                                                                               *
 *  strace-analyzer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; *
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.    *
 *  See the GNU General Public License for more details.                                         *
 *                                                                                               *
 *  You should have received a copy of the GNU General Public License along with                 *
 *  strace-analyzer. If not, see <http://www.gnu.org/licenses/>.                                 *
 *                                                                                               *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


package strace
package analyze

sealed abstract class LogEntry {
  def epoch: String
  def jepoch: Long = (epoch.toDouble * 1000).round
  def status: Int
  def time: String
}

trait HasBytes {
  self: LogEntry =>

  def bytes: Long
  def reqbytes: Long
}

trait HasFD {
  self: LogEntry =>

  def fd: String
}

object LogEntry {

  val fsSep = sys props "file.separator"

  case class Close(epoch: String, fd: String, status: Int, time: String) extends LogEntry

  object Close {
    val regex = """(\d+\.\d+) close\((\d+)\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Close] = line match {
      case regex(epoch, fd, status, time) =>
        Some(new Close(epoch, fd, status.toInt, time))
      case _ => None
    }
  }

  case class Creat(epoch: String, file: String, fd: String, time: String) extends LogEntry {
    def status = fd.toInt
  }

  object Creat {
    val regex = """(\d+\.\d+) creat\("([^"]+)", .+\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Creat] = line match {
      case regex(epoch, file, status, time) =>
        Some(new Creat(epoch, file, status, time))
      case _ => None
    }
  }

  case class Dup(epoch: String, oldFd: String, newFd: String, time: String) extends LogEntry {
    def status = newFd.toInt
  }

  object Dup {
    val regex = """(\d+\.\d+) dup\((\d+)\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Dup] = line match {
      case regex(epoch, oldFd, newFd, time) =>
        Some(new Dup(epoch, oldFd, newFd, time))
      case _ => None
    }
  }

  case class Dup2(epoch: String, oldFd: String, newFd: String, time: String) extends LogEntry {
    def status = newFd.toInt
  }

  object Dup2 {
    val regex = """(\d+\.\d+) dup2\((\d+), \d+\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Dup2] = line match {
      case regex(epoch, oldFd, newFd, time) =>
        Some(new Dup2(epoch, oldFd, newFd, time))
      case _ => None
    }
  }

  case class Open(epoch: String, file: String, status: Int, time: String) extends LogEntry {
    def fd = status.toString
  }

  object Open {
    val regex = """(\d+\.\d+) open\("([^"]+)", .+\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Open] = line match {
      case regex(epoch, file, status, time) =>
        Some(new Open(epoch, file, status.toInt, time))
      case _ => None
    }
  }

  case class OpenAt(epoch: String, wherefd: String, filename: String, status: Int, time: String) extends LogEntry {
    def file(path: String) = s"""$path$fsSep$filename"""
    def fd = status.toString
  }

  object OpenAt {
    val regex = """(\d+\.\d+) openat\((\d+|AT_FDCWD), "([^"]+)", .+\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[OpenAt] = line match {
      case regex(epoch, wherefd, file, status, time) =>
        Some(new OpenAt(epoch, wherefd, file, status.toInt, time))
      case _ => None
    }
  }

  case class Pipe(epoch: String, read: String, write: String, status: Int, time: String) extends LogEntry

  object Pipe {
    val regex = """(\d+\.\d+) pipe\(\[(\d+), (\d+)\]\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Pipe] = line match {
      case regex(epoch, read, write, status, time) =>
        Some(new Pipe(epoch, read, write, status.toInt, time))
      case _ => None
    }
  }

  case class PRead(epoch: String, fd: String, reqbytes: Long, bytes: Long, time: String)
      extends LogEntry with HasBytes with HasFD {
    def status = bytes.toInt
  }

  object PRead {
    val regex = """(\d+\.\d+) pread\((\d+),.*, (\d+), \d+\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[PRead] = line match {
      case regex(epoch, fd, reqbytes, bytes, time) =>
        Some(new PRead(epoch, fd, reqbytes.toLong, bytes.toLong, time))
      case _ => None
    }
  }

  case class PWrite(epoch: String, fd: String, reqbytes: Long, bytes: Long, time: String)
      extends LogEntry with HasBytes with HasFD {
    def status = bytes.toInt
  }

  object PWrite {
    val regex = """(\d+\.\d+) pwrite\((\d+),.*, (\d+), \d+\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[PWrite] = line match {
      case regex(epoch, fd, reqbytes, bytes, time) =>
        Some(new PWrite(epoch, fd, reqbytes.toLong, bytes.toLong, time))
      case _ => None
    }
  }

  case class Read(epoch: String, fd: String, reqbytes: Long, bytes: Long, time: String)
      extends LogEntry with HasBytes with HasFD {
    def status = bytes.toInt
  }

  object Read {
    val regex = """(\d+\.\d+) read\((\d+),.*, (\d+)\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Read] = line match {
      case regex(epoch, fd, reqbytes, bytes, time) =>
        Some(new Read(epoch, fd, reqbytes.toLong, bytes.toLong, time))
      case _ => None
    }
  }

  case class Write(epoch: String, fd: String, reqbytes: Long, bytes: Long, time: String)
      extends LogEntry with HasBytes with HasFD {
    def status = bytes.toInt
  }

  object Write {
    val regex = """(\d+\.\d+) write\((\d+),.*, (\d+)\)\s+= (\d+) <(\d+\.\d+)>""".r
    def unapply(line: String): Option[Write] = line match {
      case regex(epoch, fd, reqbytes, bytes, time) =>
        Some(new Write(epoch, fd, reqbytes.toLong, bytes.toLong, time))
      case _ => None
    }
  }

}
