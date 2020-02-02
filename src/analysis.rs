/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  Copyright  (C)  2015-2020  Christian Krause                              *
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

use lazy_static::lazy_static;
use regex::Regex;
use std::collections::HashMap;
use std::fs::File;
use std::io::{self, BufRead, BufReader};
use std::path::{Path, PathBuf};

use crate::config::Config;
use crate::log::*;
use crate::summary::Summary;

pub fn run(input: String, config: Config) -> io::Result<()> {
    let input = Path::new(&input);

    let stdin = Summary::new("STDIN");
    let stdout = Summary::new("STDOUT");
    let stderr = Summary::new("STDERR");

    let mut fds: HashMap<u32, Summary> = HashMap::new();

    fds.insert(0, stdin);
    fds.insert(1, stdout);
    fds.insert(2, stderr);

    analyze(&mut fds, input, &config)
}

fn analyze(
    fds: &mut HashMap<u32, Summary>,
    input: &Path,
    config: &Config,
) -> io::Result<()> {
    let file = File::open(input)?;

    lazy_static! {
        static ref RE_CLONE: Regex =
            Regex::new(r#"^clone\(.*\)\s+= (\d+)$"#).unwrap();
    }

    lazy_static! {
        static ref RE_CLOSE: Regex =
            Regex::new(r#"^close\((\d+)\)\s+= (-?\d+)\s*([A-Z]*).*$"#)
                .unwrap();
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
        static ref RE_OPENAT: Regex = Regex::new(
            r#"^openat\((\d+|AT_FDCWD), "([^"]+)", .+\)\s+= (\d+)$"#
        )
        .unwrap();
    }

    lazy_static! {
        static ref RE_PIPE: Regex =
            Regex::new(r#"^pipe\(\[(\d+), (\d+)\]\)\s+= (\d+)$"#).unwrap();
    }

    lazy_static! {
        static ref RE_PREAD: Regex =
            Regex::new(r#"^pread\((\d+),.*, (\d+), \d+\)\s+= (\d+)$"#)
                .unwrap();
    }

    lazy_static! {
        static ref RE_PWRITE: Regex =
            Regex::new(r#"^pwrite\((\d+),.*, (\d+), \d+\)\s+= (\d+)$"#)
                .unwrap();
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

    for l in BufReader::new(file).lines() {
        let line = l?;

        for cap in RE_CREAT.captures_iter(&line) {
            let file = &cap[1];
            let fd: u32 = cap[2].parse().unwrap();

            debug(format!("[creat] {} => {}", fd, file), config);

            let syscall = "creat";
            insert(fds, fd, Summary::new(file), syscall, config);
        }

        for cap in RE_CLOSE.captures_iter(&line) {
            let fd: u32 = cap[1].parse().unwrap();
            let status: i32 = cap[2].parse().unwrap();
            let error = &cap[3];
            let syscall = "close";

            match (status, error) {
                (0, _) => finish(fds, fd, syscall, config),

                (_, "EBADF") => {
                    debug(format!("[close] {} => bad fd", fd), config)
                }

                (_, error) => {
                    verbose(format!("[close] {} => {}", fd, error), config);
                    finish(fds, fd, syscall, config)
                }
            }
        }

        for cap in RE_CLONE.captures_iter(&line) {
            let pid = &cap[1];

            let trace = Path::new(&input).with_extension(pid);

            verbose(
                format!("[clone] tracing pid {} in {:?} ...", pid, trace),
                config,
            );

            let mut cfds = fds.clone();

            for (_, summary) in cfds.iter_mut() {
                summary.reset();
            }

            analyze(&mut cfds, &trace, config)?;

            verbose(format!("[clone] tracing pid {} finished", pid), config);
        }

        for cap in RE_DUP.captures_iter(&line) {
            let oldfd: u32 = cap[1].parse().unwrap();
            let newfd: u32 = cap[2].parse().unwrap();

            dup(fds, "dup", oldfd, newfd, config);
        }

        for cap in RE_DUP2.captures_iter(&line) {
            let oldfd: u32 = cap[1].parse().unwrap();
            let newfd: u32 = cap[2].parse().unwrap();

            dup(fds, "dup2", oldfd, newfd, config);
        }

        for cap in RE_FCNTL_DUP.captures_iter(&line) {
            let oldfd: u32 = cap[1].parse().unwrap();
            let newfd: u32 = cap[2].parse().unwrap();

            dup(fds, "fcntl-dup", oldfd, newfd, config);
        }

        for cap in RE_OPEN.captures_iter(&line) {
            let file = &cap[1];
            let fd: u32 = cap[2].parse().unwrap();

            debug(format!("[open] {} => {}", fd, file), config);

            let syscall = "open";
            insert(fds, fd, Summary::new(file), syscall, config);
        }

        for cap in RE_OPENAT.captures_iter(&line) {
            let dirfd = &cap[1];
            let pathname = &cap[2];
            let fd: u32 = cap[3].parse().unwrap();

            let file = join_paths(fds, dirfd, pathname);

            debug(format!("[openat] {} => {}", fd, file), config);

            let syscall = "openat";
            insert(fds, fd, Summary::new(&file), syscall, config);
        }

        for cap in RE_PIPE.captures_iter(&line) {
            let readend = cap[1].parse().unwrap();
            let writeend = cap[2].parse().unwrap();

            debug(format!("[pipe] {} => {}", readend, writeend), config);

            let syscall = "pipe";
            insert(fds, readend, Summary::pipe(), syscall, config);
            insert(fds, writeend, Summary::pipe(), syscall, config);
        }

        for cap in RE_PREAD.captures_iter(&line) {
            let fd: u32 = cap[1].parse().unwrap();

            if let Some(summary) = fds.get_mut(&fd) {
                let opsize: u64 = cap[2].parse().unwrap();
                let bytes: u64 = cap[3].parse().unwrap();
                summary.update_read(opsize, bytes);
            } else {
                verbose(format!("[pread] unknown fd {}", fd), config);
            }
        }

        for cap in RE_PWRITE.captures_iter(&line) {
            let fd: u32 = cap[1].parse().unwrap();

            if let Some(summary) = fds.get_mut(&fd) {
                let opsize: u64 = cap[2].parse().unwrap();
                let bytes: u64 = cap[3].parse().unwrap();
                summary.update_write(opsize, bytes);
            } else {
                verbose(format!("[pwrite] unknown fd {}", fd), config);
            }
        }

        for cap in RE_READ.captures_iter(&line) {
            let fd: u32 = cap[1].parse().unwrap();

            if let Some(summary) = fds.get_mut(&fd) {
                let opsize: u64 = cap[2].parse().unwrap();
                let bytes: u64 = cap[3].parse().unwrap();
                summary.update_read(opsize, bytes);
            } else {
                verbose(format!("[read] unknown fd {}", fd), config);
            }
        }

        for cap in RE_SOCKET.captures_iter(&line) {
            let fd: u32 = cap[1].parse().unwrap();

            debug(format!("[socket] {}", fd), config);

            let syscall = "socket";
            insert(fds, fd, Summary::socket(), syscall, config);
        }

        for cap in RE_WRITE.captures_iter(&line) {
            let fd: u32 = cap[1].parse().unwrap();

            if let Some(summary) = fds.get_mut(&fd) {
                let opsize: u64 = cap[2].parse().unwrap();
                let bytes: u64 = cap[3].parse().unwrap();
                summary.update_write(opsize, bytes);
            } else {
                verbose(format!("[write] unknown fd {}", fd), config);
            }
        }
    }

    for (_, summary) in fds.iter() {
        summary.show(config);
    }

    Ok(())
}

// ----------------------------------------------------------------------------
// helpers
// ----------------------------------------------------------------------------

fn dup(
    fds: &mut HashMap<u32, Summary>,
    syscall: &str,
    oldfd: u32,
    newfd: u32,
    config: &Config,
) {
    let summary = if let Some(summary_old) = fds.get(&oldfd) {
        let old_file = &summary_old.file;

        debug(
            format!("[{}] {} -> {} => {}", syscall, oldfd, &newfd, old_file),
            config,
        );

        Summary::new(old_file)
    } else {
        debug(
            format!("[{}] couldn't find oldfd {}", syscall, oldfd),
            config,
        );

        Summary::new("DUP")
    };

    insert(fds, newfd, summary, syscall, config);
}

fn finish(
    fds: &mut HashMap<u32, Summary>,
    fd: u32,
    syscall: &str,
    config: &Config,
) {
    if let Some(summary) = fds.remove(&fd) {
        debug(format!("[{}] {} => {}", syscall, fd, summary.file), config);

        summary.show(config);
    } else {
        verbose(format!("[{}] unknown fd {}", syscall, fd), config);
    }
}

fn insert(
    fds: &mut HashMap<u32, Summary>,
    fd: u32,
    summary: Summary,
    syscall: &str,
    config: &Config,
) {
    if let Some(summary) = fds.insert(fd, summary) {
        debug(
            format!(
                "[{}] dropping {} without explicit close",
                syscall, summary.file
            ),
            config,
        );

        summary.show(config)
    };
}

fn join_paths(
    fds: &HashMap<u32, Summary>,
    dirfd: &str,
    pathname: &str,
) -> String {
    match dirfd {
        "AT_FDCWD" => String::from(pathname),
        fd_str => {
            let dirfd: u32 = fd_str.parse().unwrap();

            if let Some(dir_summary) = fds.get(&dirfd) {
                let mut path = PathBuf::new();
                path.push(dir_summary.file.clone());
                path.push(pathname);

                if let Some(path) = path.to_str() {
                    String::from(path)
                } else {
                    String::from(pathname)
                }
            } else {
                String::from(pathname)
            }
        }
    }
}
