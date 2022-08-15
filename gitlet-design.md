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
## Persistence
