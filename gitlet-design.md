# Gitlet Design Document

**Name**: krmmzs

## Note

use TreeMap instead of HashMap when serializing.
[HashMap serialization and deserialization changes](https://stackoverflow.com/questions/5993752/hashmap-serialization-and-deserialization-changes)

## Classes and Data Structures

By chronological and logical order.

### Class Repository

Abstraction for a repository

TODO: consider static or not.

### Class Stage

The staging area(git implemented in the form of the index file).

#### Fields

1. HashSet<String> removed: (removed files)
2. HashMap<String, String>: (<file name, blob's id(SHA-1))

### Class Blob

A file.

#### Fields

1. 
2. Field 2


## Algorithms

## Persistence

```
.gitlet
    -- stageArea
	-- [stage]
    -- objects
        -- blobs and commits
	-- refs
		-- heads -> [master][branch name]
		-- remotes
			-- [remote git repo name] -> [master][branch name]
	-- [HEAD]
	-- [FETCH_HEAD]
```
- staging directory : stores staged(added) blob file; name is blob id, content is the Blob object.
- stage file: stores Stage object.
- blobs directory: stores all tracked(committed) file; name is blob id, content is the Blob object.
- commits directory: stores all commits; name is commit id, content is the Commit object.
- heads directory in refs : stores different branch; name is branch name, content is the commit id on the tip of the branch.
- remotes directory in refs: stores different remote repo directory.
- HEAD file: stores current branch's name if it points to tip.
- config file: remote git name & url.
