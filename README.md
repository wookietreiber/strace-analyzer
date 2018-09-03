strace-analyzer
===============

[![Build Status](https://travis-ci.com/wookietreiber/strace-analyzer.svg?branch=master)](https://travis-ci.com/wookietreiber/strace-analyzer)

Analyzes [strace][] output.


Table of Contents
-----------------

<!-- toc -->

- [Usage](#usage)
  * [Issues, Features, Use-Cases, Wish List](#issues-features-use-cases-wish-list)
  * [Features that will not be implemented](#features-that-will-not-be-implemented)
- [Installation](#installation)
  * [Arch Linux](#arch-linux)
  * [cargo install](#cargo-install)
  * [from source](#from-source)

<!-- tocstop -->


Usage
-----

Create logs:

```bash
strace -s 0 -ff -o cmd.strace cmd
```

Analyze logs, with `xxx` being the first process ID, the analysis will follow
forked processes automatically:

```bash
strace-analyzer cmd.strace.xxx
```

**Note:** Does only work with traces created with the usage example above.
There is no support for logs that contain output of multiple process IDs and
the timed output variants are supported neither. The above-mentioned `strace`
command line yields the shortest output and allows tracing the forked processes
without too much effort.

### Issues, Features, Use-Cases, Wish List

-   If you think of a new (possibly high-level) analysis use case or how to
    improve an existing one, please [open an issue][newissue]. If you have an
    idea on how the output should look like, feel free to include a sketch of
    an example.

-   If you recognize missing file associations in the output, i.e. bare file
    descriptor numbers without a note as to why it could not be identified,
    please [open an issue][newissue] and provide access to that particular,
    complete strace log, so I am able to identify the problem.

    If you know that a particular file should be included, because you know
    that file has been opened, it would be of great help if you would name
    these files in the issue.

### Features that will not be implemented

In the spirit of the Unix philosohpy of **do one thing and do it well**,
strace-analyzer will **not** do any of the following:

-   *filtering*, use tools like [grep][] or [awk][], e.g.:

    ```bash
    strace-analyzer cmd.strace.1835 | grep pattern
    strace-analyzer cmd.strace.1835 | awk '/pattern/'
    ```

-   *sorting*, use the [sort][] command line utility, e.g.:

    ```bash
    strace-analyzer cmd.strace.27049 | sort -k9
    ```

-   pretty *tabular output*, use the [column][] command line utility, e.g.:

    ```bash
    strace-analyzer read strace.log.27049 | column -t
    ```


Installation
------------

### Arch Linux

Install the [strace-analyzer AUR package][aur-package]:

```bash
pacaur -S strace-analyzer
```

### cargo install

```bash
cargo install strace-analyzer
```

### from source

```bash
git clone https://github.com/wookietreiber/strace-analyzer.git
cd strace-analyzer
cargo build --release
install -Dm755 target/release/strace-analyzer ~/bin/strace-analyzer
```


[aur-package]: https://aur.archlinux.org/packages/strace-analyzer "strace-analyzer AUR package"
[awk]: http://man7.org/linux/man-pages/man1/gawk.1.html "gawk man page"
[column]: http://man7.org/linux/man-pages/man1/column.1.html "column man page"
[grep]: http://man7.org/linux/man-pages/man1/grep.1.html "grep man page"
[newissue]: https://github.com/wookietreiber/strace-analyzer/issues "issue tracker"
[rust]: https://www.rust-lang.org/ "rust programming language home page"
[sort]: http://man7.org/linux/man-pages/man1/sort.1.html "sort man page"
[strace]: http://sourceforge.net/projects/strace/ "strace home page"
