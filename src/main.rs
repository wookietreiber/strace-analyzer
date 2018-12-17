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


extern crate atty;
extern crate bytesize;
#[macro_use]
extern crate clap;
#[macro_use]
extern crate lazy_static;
extern crate regex;

mod app;
mod analysis;
mod config;
mod log;
mod summary;

use analysis::analyze;
use summary::Summary;

use std::collections::HashMap;
use std::io;
use std::path::Path;

fn main() -> io::Result<()> {
    let (input, config) = app::config();

    let input = Path::new(&input);

    let stdin = Summary::new(String::from("STDIN"));
    let stdout = Summary::new(String::from("STDOUT"));
    let stderr = Summary::new(String::from("STDERR"));

    let mut fds: HashMap<u32, Summary> = HashMap::new();

    fds.insert(0, stdin);
    fds.insert(1, stdout);
    fds.insert(2, stderr);

    analyze(&mut fds, input, &config)
}
