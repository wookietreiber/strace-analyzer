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


mod analysis;
mod config;
mod log;
mod summary;

use analysis::analyze;
use config::Config;
use summary::Summary;

extern crate atty;
extern crate bytesize;
#[macro_use]
extern crate clap;
#[macro_use]
extern crate lazy_static;
extern crate regex;

use atty::Stream;
use clap::{App, AppSettings, Arg};
use std::collections::HashMap;
use std::io;
use std::path::Path;

fn main() -> io::Result<()> {
    let color = if atty::is(Stream::Stdout) {
        AppSettings::ColoredHelp
    } else {
        AppSettings::ColorNever
    };

    let matches = App::new(crate_name!())
        .version(crate_version!())
        .about(crate_description!())
        .after_help("create traces with: strace -s 0 -ff -o cmd.strace cmd")
        .global_setting(color)
        .max_term_width(80)
        .help_short("?")
        .arg(Arg::with_name("file")
             .help("strace output file name")
             .long_help("The primary output file name of the strace run. \
                         Will follow other files created via the strace -ff \
                         flag, based on the clone syscall.")
             .required(true)
             .validator(is_file))
        .arg(Arg::with_name("debug")
             .long("debug")
             .help("debug output"))
        .arg(Arg::with_name("verbose")
             .short("v")
             .long("verbose")
             .help("verbose output"))
        .get_matches();

    let input = Path::new(matches.value_of("file").unwrap());

    let config = Config {
        debug: matches.is_present("debug"),
        verbose: matches.is_present("verbose"),
    };

    let stdin = Summary::new(String::from("STDIN"));
    let stdout = Summary::new(String::from("STDOUT"));
    let stderr = Summary::new(String::from("STDERR"));

    let mut fds: HashMap<u32, Summary> = HashMap::new();

    fds.insert(0, stdin);
    fds.insert(1, stdout);
    fds.insert(2, stderr);

    analyze(&mut fds, input, &config)
}

fn is_file(s: String) -> Result<(), String> {
    let path = Path::new(&s);

    if !path.exists() {
        Err(format!("does not exist: {:?}", path))
    } else if !path.is_file() {
        Err(format!("is not a file: {:?}", path))
    } else {
        Ok(())
    }
}
