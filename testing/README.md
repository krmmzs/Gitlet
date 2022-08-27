# TESTING

## Manual Testing

Another way to test your version-control system is to compile the files and manually run commands as if it were any other version-control system (for example, java gitlet.Main init, java gitlet.Main add wug.txt, etc.). Note that based on how this project is structured with the gitlet package, you can only create a .gitlet repository in your proj2 directory and treat your proj2 directory as the working directory. If you’d like to manually test your project in a different directory, you can do so by doing the following in the proj2 directory:

```shell
javac gitlet/*.java
mkdir ~/test-gitlet
mkdir ~/test-gitlet/gitlet
cp gitlet/*.class ~/test-gitlet/gitlet
```
This will place a directory called test-gitlet in your home directory, along with all the necessary files to run your version-control system. You can then cd into this directory, and start running commands!

```shell
cd ~/test-gitlet
java gitlet.Main init
java gitlet.Main status
...
```
If you’d like to modify your gitlet code and manually test this new version, you will need to remove the .gitlet directory and replace all the .class files. You can do so with the following commands from your proj2 directory:

```shell
rm ~/test-gitlet/gitlet/*.class
rm -r ~/test-gitlet/.gitlet
javac gitlet/*.java
cp gitlet/*.class ~/test-gitlet/gitlet
```
Note: Make sure when you are calling rm, you are calling it on the correct directories. Here, you want to call rm on the `.gitlet` directory in the `~/test-gitlet` directory, and not the gitlet directory in your proj2 directory with all your code in it!

After you are finished, you can delete the entire `test-gitlet` directory with the following command:
```shell
rm -r ~/test-gitlet
```

## Integration Test

```shell
make check
```

If you’d like additional information on the failed tests, such as what your program is outputting, run:
```shell
make check TESTER_FLAGS="--verbose"
```

If you’d like to run a single test, within the testing subdirectory, run the command

```shell
python3 tester.py --verbose FILE.in ...
```

keep around the directory that tester.py produces so that you can examine its files at the point the tester script detected an error.

```shell
python3 tester.py --verbose --keep FILE.in
```
