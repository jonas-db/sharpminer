# SharpMiner
A tool for mining edit-patterns from C# code.

#1 Installation: 
SrcML: https://www.srcml.org/#download

Gradle: https://gradle.org/install

#2 Execution:
Format:

`gradle run --args="PATH NUMBER_OF_COMMITS CLUSTER_TYPE"`

where

`PATH` is a path to a .git object 

`NUMBER_OF_COMMITS` is the limit of commits to be considered

`CLUSTER_TYPE` is `RelevantCluster` or `BigCluster`

For example:

`gradle run --args="a/path/to/.git 10 RelevantCluster"`

#3 Output:
A folder `out` containing the clusters and edits.