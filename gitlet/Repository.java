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

    public static String DEFAULT_BRANCH = "master";

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
        // String branchName = "master";
        File default_branch = join(HEADS_DIR, DEFAULT_BRANCH);
        writeContents(default_branch, id); // .gitlet/refs/heads/master
        writeContents(HEAD, DEFAULT_BRANCH); // .gitlet/HEAD
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

    public static void checkInit() {
        if (!GITLET_DIR.isDirectory()) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /** 
     * 1. staging the file for addition
     * 2. If the current working version of the file is identical
     * to the version in the current commit, do not stage it to be added
     * and remove it from the staging area if it is already there(
     * as can happen when a file is changed, added, and then changed
     * back to it’s original version)
     * @param fileName added file name.
     */
    public static void add(String fileName) {
        File file = join(CWD, fileName); // get file from CWD
        if (!file.exists()) {
            exit("Not in an initialized Gitlet directory.");
        }

        // gettheHeadCommit
        Commit head = getHead();
        // get the Stage
        Stage stage = readStage();

        String headBlobId = head.getBlobs().getOrDefault(fileName, ""); // using file name to find file in current Commit.
        String stageBlobId = stage.getAdded().getOrDefault(fileName, ""); // usign file name to find file in stage.

        Blob blob = new Blob(fileName, CWD); // using file name to instance this blob.
        String blobId = blob.getId();

        // HACK: maybe have more edge case.
        // the current working version of the file is identical to 
        // the version in the current commit do not stage it be added.
        // and remove it from the staging area if it is already there
        if (blobId.equals(headBlobId)) {
            if (!blob.equals(stageBlobId)) {
                // delete the file from staging
                join(STAGING_DIR, stageBlobId).delete();
                stage.getAdded().remove(fileName);
                stage.getRemoved().remove(fileName);
                writeStage(stage);
            }
        } else if (!blob.equals(stageBlobId)) {
            // update new version

            // check no file
            if (!stageBlobId.equals("")) {
                join(STAGING_DIR, stageBlobId).delete();
            }

            writeBlobToStaging(blobId, blob);
            stage.add(fileName, blobId);
            writeStage(stage);
        }
    }


    /**
     * @param commit Commit Object which will be Serialized.
     */
    private static void writeCommitToFile(Commit commit) {
        File file = join(OBJECTS_DIR, commit.getId()); // now, without Tries firstly...
        writeObject(file, commit);
    }

    private static Commit getHead() {
        String branchName = getHeadBranchName(); // maybe master
        File branchFile = getBranchFile(branchName);
        Commit head = getCommitFromBranchFile(branchFile);

        if (head == null) {
            exit("error: can't find this branch");
        }

        return head;
    }

    private static File getBranchFile(String branchName) {
        File file = null;
        String[] branches = branchName.split("/");
        if (branches.length == 1) {
            file = join(HEADS_DIR, branchName);
        } else if (branches.length == 2) {
            file = join(REMOTES_DIR, branches[0], branches[1]);
        }
        return file;
    }

    private static Commit getCommitFromBranchFile(File file) {
        String commitId = readContentsAsString(file);
        return getCommitFromId(commitId);
    }

    private static String getHeadBranchName() {
        return readContentsAsString(HEAD);
    }

    private static Commit getCommitFromId(String commitId) {
        File file = join(OBJECTS_DIR, commitId);
        if (commitId.equals("null") || !file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    } 

    private static Stage readStage() {
        return readObject(STAGE, Stage.class);
    }

    private static void writeStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    private static void writeBlobToStaging(String blobId, Blob blob) {
        writeObject(join(STAGING_DIR, blobId), blob);
    }
}
