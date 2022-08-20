package gitlet;

import static gitlet.Utils.exit;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author krmmzs
 *
 * <pre>
 * 1. Incorporating trees into commits and not dealing with subdirectories (so there will
 * be one “flat” directory of plain files for each repository).
 *
 * 2. Limiting ourselves to merges that reference two parents (in real Git,
 * there can be any number of parents.)
 *
 * 3. Having our metadata consist only of a timestamp and log message. A commit, therefore,
 * will consist of a log message, timestamp, a mapping of file names to blob references,
 * a parent reference, and (for merges) a second parent reference.
 * <pre>
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exit("Please enter a command.");
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init" -> {
                validateNumArgs(args, 1);
                Repository.init();
            }
            case "add" -> {
                validateNumArgs(args, 2);
                Repository.checkInit();
                Repository.add(args[1]);
            }
            case "commit" -> {
                validateNumArgs(args, 2);
                Repository.checkInit();
                Repository.commit(args[1]);
            }
            case "rm" -> {
                validateNumArgs(args, 2);
                Repository.checkInit();
                Repository.rm(args[1]);
            }
            case "log" -> {
                validateNumArgs(args, 1);
                Repository.checkInit();
                Repository.log();
            }
            case "global-log" -> {
                validateNumArgs(args, 1);
                Repository.checkInit();
                Repository.global_log();
            }
            case "find" -> {
                validateNumArgs(args, 2);
                Repository.checkInit();
                Repository.find(args[1]);
            }
            default -> exit("No command with that name exists.");
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exit("Incorrect operands.");
        }
    }
}
