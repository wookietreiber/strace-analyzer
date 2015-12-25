strace-analyzer
---------------

Analyzes [strace][] output. Currently, the following analysis commands are provided:

- **summary** (default) short summary
- **read** per file read summary
- **write** per file write summary
- **io** does both **read** and **write**

File descriptors are associated with file names. The association is made when syscalls like
**open**, **creat**, **dup** or **pipe** are read from the log and the association gets terminated
when they get **close**d.

usage
-----

```bash
# create logs
strace -T -ttt -ff -o strace.log command

# analyze logs (command defines how they are analyzed)
strace-analyzer <command> strace.log.4242 strace.log.4243

# do stuff with the output
strace-analyzer read strace.log.27049 | sort -h -k 2 | column -t
```

More help can be found here:

```bash
strace --help
```

caveats
-------

-   does only work with traces created with the usage example above, there is no support for logs
    that contain output of multiple process ids

-   does not parse unfinished / resumed entries, single-threaded application runs are recommended or
    else you are going to miss a lot of entries

issues, features, use-cases, wish list
--------------------------------------

-   If you think of a new (possibly high-level) analysis use case or how to improve an existing one,
    please [open an issue][newissue]. If you have an idea on how the output should look like, feel
    free to include a sketch of an example.

-   If you recognize missing file associations in the output, i.e. bare file descriptor numbers
    without a note as to why it could not be identified, please [open an issue][newissue] and
    provide access to that particular, complete strace log, so I am able to identify the problem.

features that will not be implemented
-------------------------------------

In the spirit of the Unix philosohpy of **do one thing and do it well**, strace-analyzer will not do
any of the following:

-   filtering, use tools like [grep][] or [awk][], e.g.:

        strace-analyzer read strace.log.1835 | grep scala
        strace-analyzer read strace.log.1835 | awk '/scala/'

-   sorting, use the [sort][] command line utility, e.g.:

        strace-analyzer read strace.log.27049 | sort -h -k 2

-   pretty tabular output printing, use the [column][] command line utility, e.g.:

        strace-analyzer read strace.log.27049 | column -t

[awk]: http://man7.org/linux/man-pages/man1/gawk.1.html "gawk man page"
[grep]: http://man7.org/linux/man-pages/man1/grep.1.html "grep man page"
[column]: http://man7.org/linux/man-pages/man1/column.1.html "column man page"
[newissue]: https://github.com/wookietreiber/strace-analyzer/issues/new "open new issue"
[sort]: http://man7.org/linux/man-pages/man1/sort.1.html "sort man page"
[strace]: http://sourceforge.net/projects/strace/ "strace home page"
