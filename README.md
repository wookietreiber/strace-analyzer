strace-analyzer
===============

[![Build Status](https://travis-ci.com/wookietreiber/strace-analyzer.svg?branch=master)](https://travis-ci.com/wookietreiber/strace-analyzer)

Analyzes [strace][] output.


Table of Contents
-----------------

<!-- toc -->

- [Usage](#usage)
- [Analysis](#analysis)
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

**Note:** Only works with traces created with the usage example above. There is
no support for logs that contain output of multiple process IDs and the timed
output variants are supported neither. The above-mentioned `strace` command
line yields the shortest output and allows tracing the forked processes without
too much effort.


Analysis
--------

At the moment, `strace-analyzer` only analyzes reads and writes to the file
system:

```console
$ strace -s0 -ff -o tar.strace tar czfv pkgs.tar.gz /var/cache/pacman/pkg/linux-*
/var/cache/pacman/pkg/linux-5.12.1.arch1-1-x86_64.pkg.tar.zst
/var/cache/pacman/pkg/linux-api-headers-5.10.13-1-any.pkg.tar.zst
/var/cache/pacman/pkg/linux-docs-5.12.1.arch1-1-x86_64.pkg.tar.zst
/var/cache/pacman/pkg/linux-firmware-20210426.fa0efef-1-any.pkg.tar.zst
/var/cache/pacman/pkg/linux-lts-5.10.34-1-x86_64.pkg.tar.zst

$ strace-analyzer tar.strace.10099
Reads Bytes  Bytes/Op File
9722  94.9M  10.0K    /var/cache/pacman/pkg/linux-5.12.1.arch1-1-x86_64.pkg.tar.zst
111   1.1M   10.0K    /var/cache/pacman/pkg/linux-api-headers-5.10.13-1-any.pkg.tar.zst
2244  21.9M  10.0K    /var/cache/pacman/pkg/linux-docs-5.12.1.arch1-1-x86_64.pkg.tar.zst
17124 167.2M 10.0K    /var/cache/pacman/pkg/linux-firmware-20210426.fa0efef-1-any.pkg.tar.zst
7642  74.6M  10.0K    /var/cache/pacman/pkg/linux-lts-5.10.34-1-x86_64.pkg.tar.zst

Writes Bytes  Bytes/Op File
23020  359.7M 16.0K    pkgs.tar.gz
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
[strace]: http://sourceforge.net/projects/strace/ "strace home page"
