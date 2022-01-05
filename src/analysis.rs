/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  Copyright  (C)  2015-2022  Christian Krause                              *
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

use std::cell::RefCell;
use std::collections::HashMap;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};

use anyhow::Result;
use lazy_static::lazy_static;
use regex::{Captures, Regex};

use crate::config::Config;
use crate::log;
use crate::output::Output;
use crate::summary::{show_table, Summary};

pub fn run<P>(input: P, config: Config) -> Result<()>
where
    P: AsRef<Path> + Copy,
{
    let mut analysis = Analysis::new(config);

    match config.output {
        Output::Continuous => {
            analysis.analyze(input, |summary| summary.show(config))
        }

        Output::Table => {
            let summaries = RefCell::new(vec![]);

            analysis.analyze(input, |summary| {
                summaries.borrow_mut().push(summary);
            })?;

            show_table(&summaries.into_inner(), config);

            Ok(())
        }
    }
}

#[derive(Clone)]
struct Analysis {
    fds: HashMap<u32, Summary>,
    config: Config,
}

impl Analysis {
    fn new(config: Config) -> Self {
        let stdin = Summary::new("STDIN");
        let stdout = Summary::new("STDOUT");
        let stderr = Summary::new("STDERR");

        let mut fds: HashMap<u32, Summary> = HashMap::new();

        fds.insert(0, stdin);
        fds.insert(1, stdout);
        fds.insert(2, stderr);

        Self { fds, config }
    }

    fn analyze<F, P>(&mut self, input: P, f: F) -> Result<()>
    where
        F: Fn(Summary) + Copy,
        P: AsRef<Path> + Copy,
    {
        let file = File::open(input)?;

        for line in BufReader::new(file).lines() {
            let line = line?;

            for cap in RE_CREAT.captures_iter(&line) {
                self.syscall_creat(&cap, f);
            }

            for cap in RE_CLOSE.captures_iter(&line) {
                self.syscall_close(&cap, f);
            }

            for cap in RE_CLONE.captures_iter(&line) {
                self.syscall_clone(&cap, input, f)?;
            }

            for cap in RE_DUP.captures_iter(&line) {
                self.syscall_dup(&cap, f);
            }

            for cap in RE_DUP2.captures_iter(&line) {
                self.syscall_dup2(&cap, f);
            }

            for cap in RE_FCNTL_DUP.captures_iter(&line) {
                self.syscall_fcntl_dup(&cap, f);
            }

            for cap in RE_OPEN.captures_iter(&line) {
                self.syscall_open(&cap, f);
            }

            for cap in RE_OPENAT.captures_iter(&line) {
                self.syscall_openat(&cap, f);
            }

            for cap in RE_PIPE.captures_iter(&line) {
                self.syscall_pipe(&cap, f);
            }

            for cap in RE_PREAD.captures_iter(&line) {
                self.syscall_pread(&cap);
            }

            for cap in RE_PWRITE.captures_iter(&line) {
                self.syscall_pwrite(&cap);
            }

            for cap in RE_READ.captures_iter(&line) {
                self.syscall_read(&cap);
            }

            for cap in RE_SOCKET.captures_iter(&line) {
                self.syscall_socket(&cap, f);
            }

            for cap in RE_WRITE.captures_iter(&line) {
                self.syscall_write(&cap);
            }
        }

        for summary in self.fds.values() {
            f(summary.clone());
        }

        Ok(())
    }

    fn insert<F>(&mut self, fd: u32, summary: Summary, syscall: &str, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        if let Some(summary) = self.fds.insert(fd, summary) {
            self.debug(format!(
                "[{}] dropping {} without explicit close",
                syscall, summary.file
            ));

            f(summary);
        };
    }

    fn dup<F>(&mut self, syscall: &str, oldfd: u32, newfd: u32, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let summary = self.fds.get(&oldfd).map_or_else(
            || {
                self.debug(format!(
                    "[{}] couldn't find oldfd {}",
                    syscall, oldfd
                ));

                Summary::new("DUP")
            },
            |summary_old| {
                let old_file = &summary_old.file;

                self.debug(format!(
                    "[{}] {} -> {} => {}",
                    syscall, oldfd, &newfd, old_file
                ));

                Summary::new(old_file)
            },
        );

        self.insert(newfd, summary, syscall, f);
    }

    fn syscall_clone<F, P>(
        &mut self,
        cap: &Captures,
        input: P,
        f: F,
    ) -> Result<()>
    where
        F: Fn(Summary) + Copy,
        P: AsRef<Path>,
    {
        let pid = &cap[1];

        let trace = input.as_ref().with_extension(pid);

        self.verbose(format!(
            "[clone] tracing pid {} in {:?} ...",
            pid, trace
        ));

        let mut cloned_fds = self.fds.clone();

        for summary in cloned_fds.values_mut() {
            summary.reset();
        }

        self.clone().analyze(&trace, f)?;

        self.verbose(format!("[clone] tracing pid {} finished", pid));

        Ok(())
    }

    fn syscall_close<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let fd: u32 = cap[1].parse().unwrap();
        let status: i32 = cap[2].parse().unwrap();
        let error = &cap[3];
        let syscall = "close";

        match (status, error) {
            (0, _) => {
                self.finish(fd, syscall, f);
            }

            (_, "EBADF") => {
                self.debug(format!("[close] {} => bad fd", fd));
            }

            (_, error) => {
                self.verbose(format!("[close] {} => {}", fd, error));
                self.finish(fd, syscall, f);
            }
        }
    }

    fn syscall_creat<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let file = &cap[1];
        let fd: u32 = cap[2].parse().unwrap();

        self.debug(format!("[creat] {} => {}", fd, file));

        let syscall = "creat";
        self.insert(fd, Summary::new(file), syscall, f);
    }

    fn syscall_dup<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let oldfd: u32 = cap[1].parse().unwrap();
        let newfd: u32 = cap[2].parse().unwrap();

        self.dup("dup", oldfd, newfd, f);
    }

    fn syscall_dup2<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let oldfd: u32 = cap[1].parse().unwrap();
        let newfd: u32 = cap[2].parse().unwrap();

        self.dup("dup2", oldfd, newfd, f);
    }

    fn syscall_fcntl_dup<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let oldfd: u32 = cap[1].parse().unwrap();
        let newfd: u32 = cap[2].parse().unwrap();

        self.dup("fcntl-dup", oldfd, newfd, f);
    }

    fn syscall_open<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let file = &cap[1];
        let fd: u32 = cap[2].parse().unwrap();

        self.debug(format!("[open] {} => {}", fd, file));

        let syscall = "open";
        self.insert(fd, Summary::new(file), syscall, f);
    }

    fn syscall_openat<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let dirfd = &cap[1];
        let pathname = &cap[2];
        let fd: u32 = cap[3].parse().unwrap();

        let file = self.join_paths(dirfd, pathname);

        self.debug(format!("[openat] {} => {}", fd, file));

        let syscall = "openat";
        self.insert(fd, Summary::new(&file), syscall, f);
    }

    fn syscall_pipe<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let readend = cap[1].parse().unwrap();
        let writeend = cap[2].parse().unwrap();

        self.debug(format!("[pipe] {} => {}", readend, writeend));

        let syscall = "pipe";
        self.insert(readend, Summary::pipe(), syscall, f);
        self.insert(writeend, Summary::pipe(), syscall, f);
    }

    fn syscall_pread(&mut self, cap: &Captures) {
        let fd: u32 = cap[1].parse().unwrap();

        if let Some(summary) = self.fds.get_mut(&fd) {
            let opsize: u64 = cap[2].parse().unwrap();
            let bytes: u64 = cap[3].parse().unwrap();
            summary.update_read(opsize, bytes);
        } else {
            self.verbose(format!("[pread] unknown fd {}", fd));
        }
    }

    fn syscall_pwrite(&mut self, cap: &Captures) {
        let fd: u32 = cap[1].parse().unwrap();

        if let Some(summary) = self.fds.get_mut(&fd) {
            let opsize: u64 = cap[2].parse().unwrap();
            let bytes: u64 = cap[3].parse().unwrap();
            summary.update_write(opsize, bytes);
        } else {
            self.verbose(format!("[pwrite] unknown fd {}", fd));
        }
    }

    fn syscall_read(&mut self, cap: &Captures) {
        let fd: u32 = cap[1].parse().unwrap();

        if let Some(summary) = self.fds.get_mut(&fd) {
            let opsize: u64 = cap[2].parse().unwrap();
            let bytes: u64 = cap[3].parse().unwrap();
            summary.update_read(opsize, bytes);
        } else {
            self.verbose(format!("[read] unknown fd {}", fd));
        }
    }

    fn syscall_socket<F>(&mut self, cap: &Captures, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        let fd: u32 = cap[1].parse().unwrap();

        self.debug(format!("[socket] {}", fd));

        let syscall = "socket";
        self.insert(fd, Summary::socket(), syscall, f);
    }

    fn syscall_write(&mut self, cap: &Captures) {
        let fd: u32 = cap[1].parse().unwrap();

        if let Some(summary) = self.fds.get_mut(&fd) {
            let opsize: u64 = cap[2].parse().unwrap();
            let bytes: u64 = cap[3].parse().unwrap();
            summary.update_write(opsize, bytes);
        } else {
            self.verbose(format!("[write] unknown fd {}", fd));
        }
    }

    fn finish<F>(&mut self, fd: u32, syscall: &str, f: F)
    where
        F: Fn(Summary) + Copy,
    {
        if let Some(summary) = self.fds.remove(&fd) {
            self.debug(format!("[{}] {} => {}", syscall, fd, summary.file));
            f(summary);
        } else {
            self.verbose(format!("[{}] unknown fd {}", syscall, fd));
        }
    }

    fn join_paths(&self, dirfd: &str, pathname: &str) -> String {
        if dirfd == "AT_FDCWD" {
            String::from(pathname)
        } else {
            let dirfd: u32 = dirfd.parse().unwrap();

            self.fds.get(&dirfd).map_or_else(
                || String::from(pathname),
                |dir_summary| {
                    let mut path = PathBuf::new();
                    path.push(dir_summary.file.clone());
                    path.push(pathname);

                    path.to_str()
                        .map_or_else(|| String::from(pathname), String::from)
                },
            )
        }
    }

    fn debug<S: AsRef<str>>(&self, message: S) {
        log::debug(message, self.config);
    }

    fn verbose<S: AsRef<str>>(&self, message: S) {
        log::verbose(message, self.config);
    }
}

// ----------------------------------------------------------------------------
// regexes
// ----------------------------------------------------------------------------

lazy_static! {
    static ref RE_CLONE: Regex =
        Regex::new(r#"^clone\(.*\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_CLOSE: Regex =
        Regex::new(r#"^close\((\d+)\)\s+= (-?\d+)\s*([A-Z]*).*$"#).unwrap();
}

lazy_static! {
    static ref RE_CREAT: Regex =
        Regex::new(r#"^creat\("([^"]+)", .+\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_DUP: Regex =
        Regex::new(r#"^dup\((\d+)\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_DUP2: Regex =
        Regex::new(r#"^dup2\((\d+), \d+\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_FCNTL_DUP: Regex =
        Regex::new(r#"^fcntl\((\d+), F_DUPFD, \d+\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_OPEN: Regex = Regex::new(
        // we're ignoring failures on purpose because they don't open fd
        r#"^open\("([^"]+)", .+\)\s+= (\d+)$"#
    ).unwrap();
}

lazy_static! {
    static ref RE_OPENAT: Regex =
        Regex::new(r#"^openat\((\d+|AT_FDCWD), "([^"]+)", .+\)\s+= (\d+)$"#)
            .unwrap();
}

lazy_static! {
    static ref RE_PIPE: Regex =
        Regex::new(r#"^pipe\(\[(\d+), (\d+)\]\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_PREAD: Regex =
        Regex::new(r#"^pread\((\d+),.*, (\d+), \d+\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_PWRITE: Regex =
        Regex::new(r#"^pwrite\((\d+),.*, (\d+), \d+\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_READ: Regex =
        Regex::new(r#"^read\((\d+),.*, (\d+)\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_SOCKET: Regex =
        Regex::new(r#"^socket\(.*\)\s+= (\d+)$"#).unwrap();
}

lazy_static! {
    static ref RE_WRITE: Regex =
        Regex::new(r#"^write\((\d+),.*, (\d+)\)\s+= (\d+)$"#).unwrap();
}

// ----------------------------------------------------------------------------
// tests
// ----------------------------------------------------------------------------

#[cfg(test)]
pub mod tests {
    use std::process::Command;

    use assert_cmd::prelude::*;
    use assert_fs::prelude::*;
    use predicates::prelude::*;

    use super::*;

    #[test]
    fn analyze_dd() {
        let temp = assert_fs::TempDir::new().unwrap();

        let trace = temp.child("dd.strace");
        trace.assert(predicate::path::missing());

        let mut strace_dd = Command::new("strace");
        strace_dd.current_dir(&temp);
        strace_dd.args(["-s", "0"]);
        strace_dd.args(["-o", trace.path().to_string_lossy().as_ref()]);
        strace_dd.args([
            "dd",
            "if=/dev/zero",
            "of=/dev/null",
            "bs=1M",
            "count=1024",
            "status=none",
        ]);

        strace_dd.assert().success();
        trace.assert(predicate::path::exists());

        let config = Config::default();
        let summaries = RefCell::new(vec![]);
        let mut analysis = Analysis::new(config);

        analysis
            .analyze(trace.path(), |summary| {
                summaries.borrow_mut().push(summary);
            })
            .unwrap();

        let summaries = summaries.into_inner();

        assert!(summaries.contains(&Summary {
            file: "/dev/zero".into(),
            read_freq: HashMap::from([(1_048_576, 1024)]),
            write_freq: HashMap::default(),
            read_bytes: 1_073_741_824,
            write_bytes: 0,
        }));

        assert!(summaries.contains(&Summary {
            file: "/dev/null".into(),
            read_freq: HashMap::default(),
            write_freq: HashMap::from([(1_048_576, 1024)]),
            read_bytes: 0,
            write_bytes: 1_073_741_824,
        }));

        temp.close().unwrap();
    }
}
