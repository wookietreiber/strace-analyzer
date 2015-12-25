strace-analyzer
---------------

Analyzes [strace][] output. Currently, the following analysis commands are provided:

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

caveats, missing features
-------------------------

-   does only work with traces created with the usage example above, there is no support for logs
    that contain output of multiple process ids

-   does not parse unfinished / resumed entries, single-threaded application runs are recommended or
    else you are going to miss a lot of entries

-   this tool will not implement sorting, use the [sort][] command line utility, e.g.:

        strace-analyzer read ~/strace.log.27049 | sort -h -k 2

-   this tool will not implement pretty tabular output printing, use the [column][] command line
    utility, e.g.:

        strace-analyzer read ~/strace.log.27049 | column -t

[column]: http://man7.org/linux/man-pages/man1/column.1.html "column man page"
[sort]: http://man7.org/linux/man-pages/man1/sort.1.html "sort man page"
[strace]: http://sourceforge.net/projects/strace/ "strace home page"
