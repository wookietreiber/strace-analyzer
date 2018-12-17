use atty::Stream;
use clap::{App, AppSettings, Arg};
use std::path::Path;

use config::*;

pub fn config() -> (String, Config) {
    let args = cli_parser().get_matches();

    let input = args.value_of("file").unwrap();

    let debug = args.is_present("debug");

    let verbose = args.is_present("verbose");

    let config = Config {
        debug,
        verbose,
    };

    (String::from(input), config)
}

fn cli_parser() -> App<'static, 'static> {
    let color = atty::is(Stream::Stdout);

    let color = if color {
        AppSettings::ColoredHelp
    } else {
        AppSettings::ColorNever
    };

    App::new(crate_name!())
        .version(crate_version!())
        .about(crate_description!())
        .after_help("create traces with: strace -s 0 -ff -o cmd.strace cmd")
        .global_setting(color)
        .max_term_width(80)
        .help_short("?")
        .help_message("show this help output")
        .version_message("show version")
        .arg(Arg::with_name("file")
             .help("strace output file name")
             .long_help("The primary output file name of the strace run. \
                         strace-analyzer will follow other strace files \
                         created via the strace -ff flag. The followed files \
                         are determined based on the clone syscalls that are \
                         encountered in the traces.")
             .required(true)
             .validator(is_file))
        .arg(Arg::with_name("debug")
             .long("debug")
             .help("debug output"))
        .arg(Arg::with_name("verbose")
             .short("v")
             .long("verbose")
             .help("verbose output"))
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
