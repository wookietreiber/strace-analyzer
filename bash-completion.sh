# bash completion for strace-analyzer

_strace-analyzer()
{
    local cur prev
    _init_completion || return

    case $prev in
        summary|read|write|io)
            _expand || return 0
            compopt -o filenames
            COMPREPLY=( $( compgen -fd -- "$cur" ) )
            return 0
            ;;
    esac

    case $cur in
        --*)
            COMPREPLY=( $( compgen -W '--help --version' -- $cur ) )
            ;;
        -*)
            COMPREPLY=( $( compgen -W '-? -h -help -version --help --version' -- $cur ) )
            ;;
        *)
            COMPREPLY=( $( compgen -W 'summary read write io io-profile' -- $cur ) )
            ;;
    esac

    return 0
} &&
complete -F _strace-analyzer strace-analyzer
