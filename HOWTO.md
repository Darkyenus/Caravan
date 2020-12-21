# How to compile/run
All commands work on all three most popular OS, but a POSIX shell is required.
On macOS/Linux, all shells are POSIX shells, on Windows use Cygwin, Msys or the shell that comes with Git.

Most/all commands are Wemi commands. Wemi executable is stored directly in the repo, so to run it,
execute `./wemi` when in the appropriate directory (this one). Commands here are written in form `./wemi run`,
but it is usually faster and more ergonomic to  enter the interactive mode (`./wemi`) and then enter the commands
directly into Wemi (`run`). To leave the interactive mode, use `exit` command, Ctrl-C or Ctrl-D.

1. Pack resources - this must be done after first repository pull and then each time when resources change.
    - `./wemi packResources` this compiles resource files from `resources` to `assets`. `assets` is not in Git - do not add it!
    - For more info, see [ResourcePacker documentation](https://github.com/Darkyenus/ResourcePacker)
2. Run the thing
    - `./wemi run`
    - This automatically launches with debugger server running, so to debug create new IntelliJ Remote run configuration and just run it to connect to the running application
    - If you need to debug something that happens at startup, use `./wemi debug:run` - the application will not start before the debugger connection is established
