package gitlet;

import java.io.File;
import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  @author krmmzs
 */
public class Repository {

    /**
     * The current working directory(work tree).
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.(worktree/.git).
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The staging directory, restores staging Blobs.
     */
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");

    /** 
     * The stage Object.(replace index)
     */
    public static final File STAGE = join(GITLET_DIR, "stage");
    // /**
    //  * The Objects directory, stores committed blobs.
    //  */
    // public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    // /**
    //  * The commits directory.
    //  */
    // public static final File COMMIT_DIR = join(GITLET_DIR, "commits");

    /**
     * The Objects directory, stores commits and blobs.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "Objects");

    // The branches directory(Mimicking .git).

    /**
     * The reference directory.
     */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    /**
     * The heads directory.
     */
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    /**
     * The remotes directory.
     */
    public static final File REMOTES_DIR = join(REFS_DIR, "remotes");

    /**
     * stores current branch's name if it points to tip
     */
    public static File HEAD = join(GITLET_DIR, "HEAD");;
    // Note that in Gitlet, there is no way to be in a detached head state
    
    public static File CONFIG;

    public static void init() {
        if (GITLET_DIR.exists() && GITLET_DIR.isDirectory()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
        // create directory (.gitlet)
        createInitDir();

        // inital commit
        Commit initialCommit = new Commit();
        // initialCommit.saveCommit();
        writeCommitToFile(initialCommit);

        // create Master
        // create HEAD
        // HACK:maybe could write a Class:Branch
        String id = initialCommit.getId();
        String branchName = "master";
        File master = join(HEADS_DIR, branchName);
        writeContents(master, id); // .gitlet/refs/heads/master
        writeContents(HEAD, branchName); // .gitlet/HEAD
    }

    private static void createInitDir() {
        GITLET_DIR.mkdir();
        STAGING_DIR.mkdir();
        writeObject(STAGE, new Stage());
        OBJECTS_DIR.mkdir();
        // BLOBS_DIR.mkdir();
        // COMMIT_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();
        REMOTES_DIR.mkdir();
    }


    /**
     * @param commit Commit Object which will be Serialized.
     */
    private static void writeCommitToFile(Commit commit) {
        File file = join(COMMIT_DIR, commit.getId()); // now, without Tries firstly...
        writeObject(file, commit);
    }

}
