/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  Copyright  (C)  2015-2024  Christian Krause                              *
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

use bytesize::ByteSize;
use std::collections::HashMap;

#[cfg(feature = "table")]
use prettytable::{cell, format::FormatBuilder, Row, Table};

use crate::config::Config;
use crate::log::debug;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Summary {
    pub file: String,
    pub read_freq: HashMap<u64, u64>,
    pub write_freq: HashMap<u64, u64>,
    pub read_bytes: u64,
    pub write_bytes: u64,
}

impl Summary {
    pub fn new(file: &str) -> Self {
        Self {
            file: String::from(file),
            read_freq: HashMap::new(),
            write_freq: HashMap::new(),
            read_bytes: 0,
            write_bytes: 0,
        }
    }

    pub fn pipe() -> Self {
        Self::new("PIPE")
    }

    pub fn socket() -> Self {
        Self::new("SOCKET")
    }

    pub fn reset(&mut self) {
        self.read_freq.clear();
        self.write_freq.clear();
        self.read_bytes = 0;
        self.write_bytes = 0;
    }

    pub fn update_read(&mut self, op_size: u64, bytes: u64) {
        let freq = self.read_freq.entry(op_size).or_insert(0);
        *freq += 1;
        self.read_bytes += bytes;
    }

    pub fn update_write(&mut self, op_size: u64, bytes: u64) {
        let freq = self.write_freq.entry(op_size).or_insert(0);
        *freq += 1;
        self.write_bytes += bytes;
    }

    pub fn should_show(&self, config: Config) -> bool {
        config.verbose
            || !(self.file.starts_with("/bin/")
                || self.file == "/dev/null"
                || self.file.starts_with("/etc/")
                || self.file.starts_with("/lib/")
                || self.file.starts_with("/lib64/")
                || self.file.starts_with("/opt/")
                || self.file.starts_with("/proc/")
                || self.file.starts_with("/run/")
                || self.file.starts_with("/sbin/")
                || self.file.starts_with("/sys/")
                || self.file.starts_with("/tmp/")
                || self.file.starts_with("/usr/")
                || self.file == "STDOUT"
                || self.file == "STDERR"
                || self.file == "STDIN"
                || self.file == "SOCKET"
                || self.file == "DUP"
                || self.file == "PIPE")
    }

    pub fn show(&self, config: Config) {
        if !self.should_show(config) {
            return;
        }

        if self.read_freq.is_empty() && self.write_freq.is_empty() {
            debug(format!("no I/O with {}", self.file), config);
            return;
        }

        if !self.read_freq.is_empty() {
            let (op_size, _) = self.read_freq.iter().max().unwrap();
            let n_ops: u64 = self.read_freq.values().sum();

            println!(
                "read {} with {} ops ({} / op) {}",
                humanize(self.read_bytes),
                n_ops,
                humanize(*op_size),
                self.file,
            );
        }

        if !self.write_freq.is_empty() {
            let (op_size, _) = self.write_freq.iter().max().unwrap();
            let n_ops: u64 = self.write_freq.values().sum();

            println!(
                "write {} with {} ops ({} / op) {}",
                humanize(self.write_bytes),
                n_ops,
                humanize(*op_size),
                self.file,
            );
        }
    }
}

fn humanize(bytes: u64) -> String {
    ByteSize(bytes)
        .to_string_as(true)
        .replace("iB", "")
        .replace(' ', "")
        .to_uppercase()
}

#[cfg(feature = "table")]
pub fn show_table(summaries: &[Summary], config: Config) {
    let format = FormatBuilder::new().column_separator(' ').build();

    let mut r_table = Table::new();
    let mut w_table = Table::new();

    r_table.set_format(format);
    w_table.set_format(format);

    let mut r_titles = Row::empty();
    r_titles.add_cell(cell!(bu->"Reads"));
    r_titles.add_cell(cell!(bu->"Bytes"));
    r_titles.add_cell(cell!(bu->"Bytes/Op"));
    r_titles.add_cell(cell!(bu->"File"));
    r_table.set_titles(r_titles);

    let mut w_titles = Row::empty();
    w_titles.add_cell(cell!(bu->"Writes"));
    w_titles.add_cell(cell!(bu->"Bytes"));
    w_titles.add_cell(cell!(bu->"Bytes/Op"));
    w_titles.add_cell(cell!(bu->"File"));
    w_table.set_titles(w_titles);

    for summary in summaries {
        if !summary.should_show(config) {
            continue;
        }

        if !summary.read_freq.is_empty() {
            let mut row = Row::empty();

            let (op_size, _) = summary.read_freq.iter().max().unwrap();
            let n_ops: u64 = summary.read_freq.values().sum();

            row.add_cell(cell!(n_ops));
            row.add_cell(cell!(humanize(summary.read_bytes)));
            row.add_cell(cell!(humanize(*op_size)));
            row.add_cell(cell!(summary.file));

            r_table.add_row(row);
        }

        if !summary.write_freq.is_empty() {
            let mut row = Row::empty();

            let (op_size, _) = summary.write_freq.iter().max().unwrap();
            let n_ops: u64 = summary.write_freq.values().sum();

            row.add_cell(cell!(n_ops));
            row.add_cell(cell!(humanize(summary.write_bytes)));
            row.add_cell(cell!(humanize(*op_size)));
            row.add_cell(cell!(summary.file));

            w_table.add_row(row);
        }
    }

    if !r_table.is_empty() {
        r_table.printstd();
        println!();
    }

    if !w_table.is_empty() {
        w_table.printstd();
        println!();
    }
}
