/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *                                                                           *
 *  Copyright  (C)  2015-2023  Christian Krause                              *
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

use clap::builder::EnumValueParser;
use clap::{crate_description, crate_name, crate_version, ArgAction};
use clap::{Arg, Command};
use is_terminal::IsTerminal;
use std::path::Path;

use crate::output::Output;

pub fn build() -> Command {
    let input = Arg::new("input")
        .help("strace output file name")
        .long_help(
"The primary output file name of the strace run. strace-analyzer will follow \
 other strace files created via the strace -ff flag. The followed files are \
 determined based on the clone syscalls that are encountered in the traces."
        )
        .action(ArgAction::Set)
        .required(true)
        .value_parser(is_file);

    let output_format = Arg::new("output_format")
        .long("output")
        .help("output format")
        .long_help("Specify output format of the report.")
        .action(ArgAction::Set)
        .ignore_case(true)
        .value_parser(EnumValueParser::<Output>::new())
        .display_order(1);

    let output_format =
        if cfg!(feature = "table") && std::io::stdin().is_terminal() {
            output_format.default_value("table")
        } else {
            output_format.default_value("continuous")
        };

    let debug = Arg::new("debug")
        .long("debug")
        .action(ArgAction::SetTrue)
        .long_help("Show debug output.")
        .hide_short_help(true);

    let verbose = Arg::new("verbose")
        .short('v')
        .long("verbose")
        .action(ArgAction::SetTrue)
        .help("verbose output");

    let help = Arg::new("help")
        .short('?')
        .long("help")
        .help("print help (use --help to see all options)")
        .long_help("Print help.")
        .action(ArgAction::Help);

    let version = Arg::new("version")
        .long("version")
        .long_help("Print version.")
        .hide_short_help(true)
        .action(ArgAction::Version);

    Command::new(crate_name!())
        .version(crate_version!())
        .about(crate_description!())
        .after_help("create traces with: strace -s 0 -ff -o cmd.strace cmd")
        .max_term_width(80)
        .arg(input)
        .arg(output_format)
        .arg(debug)
        .arg(verbose)
        .disable_help_flag(true)
        .disable_version_flag(true)
        .arg(help)
        .arg(version)
}

fn is_file(s: &str) -> Result<String, String> {
    let path = Path::new(&s);

    if !path.exists() {
        Err(format!("does not exist: {}", path.display()))
    } else if path.is_file() {
        Ok(String::from(s))
    } else {
        Err(format!("is not a file: {}", path.display()))
    }
}

// ----------------------------------------------------------------------------
// tests
// ----------------------------------------------------------------------------

#[cfg(test)]
mod test {
    #[test]
    fn verify_cli() {
        super::build().debug_assert();
    }
}
