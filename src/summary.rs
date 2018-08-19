/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  Copyright  (C)  2015-2018  Christian Krause                              *
 *                                                                           *
 *  Christian Krause  <christian.krause@mailbox.org>                         *
 *                                                                           *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  This file is part of strace-analyzer.                                    *
 *                                                                           *
 *  strace-analyzer is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by     *
 *  the Free Software Foundation, either version 3 of the license, or any    *
 *  later version.                                                           *
 *                                                                           *
 *  strace-analyzer is distributed in the hope that it will be useful, but   *
 *  WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU         *
 *  General Public License for more details.                                 *
 *                                                                           *
 *  You should have received a copy of the GNU General Public License along  *
 *  with strace-analyzer. If not, see <http://www.gnu.org/licenses/>.        *
 *                                                                           *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


use config::Config;
use log::*;

use bytesize::ByteSize;

#[derive(Clone)]
#[derive(Debug)]
pub struct Summary {
    pub file: String,
    read_ops: u64,
    write_ops: u64,
    read_bytes: u64,
    write_bytes: u64,
}

impl Summary {
    pub fn new(file: String) -> Summary {
        Summary {
            file,
            read_ops: 0,
            write_ops: 0,
            read_bytes: 0,
            write_bytes: 0,
        }
    }

    pub fn pipe() -> Summary {
        Summary::new(String::from("PIPE"))
    }

    pub fn socket() -> Summary {
        Summary::new(String::from("SOCKET"))
    }

    pub fn reset(&mut self) {
        self.read_ops = 0;
        self.write_ops = 0;
        self.read_bytes = 0;
        self.write_bytes = 0;
    }

    pub fn update_read(&mut self, bytes: u64) {
        self.read_ops += 1;
        self.read_bytes += bytes;
    }

    pub fn update_write(&mut self, bytes: u64) {
        self.write_ops += 1;
        self.write_bytes += bytes;
    }

    pub fn show(&self, config: &Config) {
        if !config.verbose &&
            (self.file == "/dev/null" ||
             self.file.starts_with("/etc") ||
             self.file.starts_with("/usr") ||
             self.file == "STDOUT" ||
             self.file == "STDERR" ||
             self.file == "STDIN" ||
             self.file == "SOCKET" ||
             self.file == "DUP" ||
             self.file == "PIPE") {
                return;
            }

        if self.read_ops == 0 && self.write_ops == 0 {
            debug(format!("no I/O with {}", self.file), config);
            return;
        }

        if self.read_ops > 0 {
            let read = ByteSize(self.read_bytes).to_string_as(true);
            let op_size = self.read_bytes / self.read_ops;
            let mean = ByteSize(op_size).to_string_as(true);

            println!(
                "read {} with {} ops ({} / op) {}",
                read,
                self.read_ops,
                mean,
                self.file,
            );
        }

        if self.write_ops > 0 {
            let write = ByteSize(self.write_bytes).to_string_as(true);
            let op_size = self.write_bytes / self.write_ops;
            let mean = ByteSize(op_size).to_string_as(true);

            println!(
                "write {} with {} ops ({} / op) {}",
                write,
                self.write_ops,
                mean,
                self.file,
            );
        }
    }
}
