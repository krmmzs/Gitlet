# Gitlet
An independant implementation of a mini version of Git

## Command

### init

Usage: `java gitlet.Main init`

Creates a new Gitlet version-control system in the current directory

### add

Usage: `java gitlet.Main add [file name]`

Adds a copy of the file as it currently exists to the staging area

Differences from real git: In real git, multiple files may be added at once. In gitlet, only one file may be added at a time.

### commit

Usage: `java gitlet.Main commit [message]`

Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, creating a new commit.

Differences from real git: In real git, commits may have multiple parents (due to merging) and also have considerably more metadata.

### rm

Usage: `java gitlet.Main rm [file name]`

### log

Usage: `java gitlet.Main log`

Starting at the current head commit, display information about each commit backwards along the commit tree until the initial commit, following the first parent commit links, ignoring any second parents found in merge commits.

### global-log

Usage: `java gitlet.Main global-log`

Like log, except displays information about all commits ever made. 

### find

Usage: `java gitlet.Main find [commit message]`

Prints out the ids of all commits that have the given commit message, one per line.

Differences from real git: Doesn’t exist in real git. Similar effects can be achieved by grepping the output of log.

### status

Usage: `java gitlet.Main status`

Displays what branches currently exist, and marks the current branch with a *. Also displays what files have been staged for addition or removal.

### checkout

Usages:
1. `java gitlet.Main checkout -- [file name]`
2. `java gitlet.Main checkout [commit id] -- [file name]`
3. `java gitlet.Main checkout [branch name]`

Differences from real git: Real git does not clear the staging area and stages the file that is checked out. Also, it won’t do a checkout that would overwrite or undo changes (additions or removals) that you have staged.

### branch

Usage: `java gitlet.Main branch [branch name]`

Creates a new branch with the given name, and points it at the current head commit.

### reset

Usage: `java gitlet.Main reset [commit id]`

Differences from real git: This command is closest to using the --hard option, as in git reset --hard [commit hash].

### merge

Usage: `java gitlet.Main merge [branch name]`

Differences from real git:
Real Git does a more subtle job of merging files, displaying conflicts only in places where both files have changed since the split point.
Real Git has a different way to decide which of multiple possible split points to use.
Real Git will force the user to resolve the merge conflicts before committing to complete the merge. Gitlet just commits the merge, conflicts and all, so that you must use a separate commit to resolve problems.
Real Git will complain if there are unstaged changes to a file that would be changed by a merge. You may do so as well if you want, but we will not test that case.

### add-remote

Usage: `java gitlet.Main add-remote [remote name] [name of remote directory]/.gitlet`

Saves the given login information under the given remote name.

### rm-remote

Usage: `java gitlet.Main rm-remote [remote name]`

Remove information associated with the given remote name.

### push

Usage: `java gitlet.Main push [remote name] [remote branch name]`

Attempts to append the current branch’s commits to the end of the given branch at the given remote.

### fetch

Usage: `java gitlet.Main fetch [remote name] [remote branch name]`

Brings down commits from the remote Gitlet repository into the local Gitlet repository.

### pull

Usage: `java gitlet.Main pull [remote name] [remote branch name]`

Fetches branch `[remote name]/[remote branch name]` as for the `fetch` command, and then merges that fetch into the current branch.

