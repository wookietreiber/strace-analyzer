% STRACE-ANALYZER(1) Version 0.2.0 | strace-analyzer manual
%
% March 2016

# NAME

strace-analyzer - analyzes strace output

# SYNOPSIS

| **strace-analyzer** \<*command*> \[*log1* *log2* ...]

# DESCRIPTION

**strace-analyzer** analyzes logs created by **strace(1)**. There are different analysis commands
which are explained in the **Analysis Commands** section.

The file descriptors you see when looking at the logs manually are not very descriptive, i.e. to
know what happened with which file, pipe, etc. For this reason, **strace-analyzer** associates these
file descriptors with file names. These associations are made when syscalls like **open(2)**,
**creat(2)**, **dup(2)** or **pipe(2)** are read from the log and are terminated when they get
**close(2)**d.

There are some **strace(1)** command line options that are required for **strace-analyzer** to
interpret the logs correctly. The option **-T** is required to trace the time spent on the syscalls
themselves. The option **-ttt** is required to trace the syscalls over time. The option **-o** is
required to store the logs on disk. The option **-ff** is required for each process to get its own
log file. The problem with strace and writing syscalls from multiple processes (or threads) to the
same output file is that start and end of a single syscall are split over two lines and there is no
way of merging them back together with respect to the file descriptor association. Thus, the
resulting **strace(1)** invocation to create the logs should look as in the **EXAMPLES** section.

## Analysis Commands

help

:   Prints usage information.

summary

:   Prints a summary for the syscalls **read(2)** and **write(2)**.

read

:   Prints a **read(2)** summary for each file.

write

:   Prints a **write(2)** summary for each file.

io

:   Does both the **read** and **write** commands for each file.

io-profile

:   Per file **read** and **write** profile. Outputs a chart (png) per file and per operation.

# OPTIONS

-?, -h, -help, --help

:   Prints usage information.

-version, --version

:   Prints the current version number.

# EXAMPLES

## create logs

strace -T -ttt -ff -o /tmp/strace-dd.log dd if=/dev/zero of=/dev/null count=1024 bs=1M

## analyze logs

strace-analyzer io /tmp/strace-dd.log.*

## analyze logs with filtering and pretty, tabular printing

strace-analyzer io /tmp/strace-dd.log.* | grep /dev/ | column -t

# BUGS, ISSUES and FEATURE REQUESTS

See GitHub issues: <https://github.com/wookietreiber/strace-analyzer/issues>

# AUTHOR

Christian Krause <https://github.com/wookietreiber>

# SEE ALSO

**strace(1)**
