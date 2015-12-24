strace-analyzer
---------------

Analyzes strace output. Currently, it does the following:

-   associate file descriptors with file names via an internal database
    - db entries are added, e.g. when syscalls **creat**, **dup**,  **open**, **pipe** are read from
      the log
    - db entries are removed, e.g. when syscalls **close** are read from the log
-   output for each file / file descriptor how much data has been read / written in what amount of
    time

Later, there will be options on what stats to output in what detail, etc.

usage
-----

```bash
# create logs
strace -T -ttt -ff -o strace.log command

# analyze logs (command defines how they are analyzed)
strace-analyzer <command> strace.log.4242 strace.log.4243
```

More help can be found here:

```bash
strace --help
```

caveats
-------

- does only work with traces created with the usage example above, there is no support yet for logs
  that contain output of multiple process ids
- does not parse unfinished / resumed entries, so single-threaded is recommended or else you are
  going to miss a lot of entries
