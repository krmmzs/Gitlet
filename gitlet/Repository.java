package gitlet;

import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


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

    /**
     * The Objects directory, stores commits and blobs.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "Objects");

    /**
     * The Objects directory, stores committed blobs.
     */
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /**
     * The commits directory.
     */
    public static final File COMMIT_DIR = join(OBJECTS_DIR, "commits");

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
        BLOBS_DIR.mkdir();
        COMMIT_DIR.mkdir();
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

        Blob blob = new Blob(fileName, CWD); // using file name to instance this blob.
        String blobId = blob.getId();

        // gettheHeadCommit
        Commit head = getHead();
        // get the Stage
        Stage stage = readStage();

        String headBlobId = head.getBlobs().getOrDefault(fileName, ""); // using file name to find file in current Commit.
        String stageBlobId = stage.getAdded().getOrDefault(fileName, ""); // usign file name to find file in stage.

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

    public static void commit(String msg) {
        if (msg.equals("")  ) {
            exit("Please enter a commit message.");
        }
        // TODO: get the current commit
        // TODO: get the stage
        // gettheHeadCommit
        Commit head = getHead();
        commitWith(msg, List.of(head));
    }

    public static void rm(String fileName) {
        File file = join(CWD, fileName);

        Commit head = getHead();
        Stage stage = readStage();

        String headBlobId = head.getBlobs().getOrDefault(fileName, "");
        String stageBlobId = stage.getAdded().getOrDefault(fileName, "");

        if (headBlobId.equals("") && stageBlobId.equals("")) {
            exit("No reason to remove the file.");
        }

        // Unstage the file if it is currently staged for addition.
        if (!stageBlobId.equals("")) {
            stage.getAdded().remove(fileName);
        } else {
            // stage it for removal
            stage.getRemoved().add(fileName);
        }

        Blob blob = new Blob(fileName, CWD);
        String blobId = blob.getId();
        // If the file is tracked in the current
        // commit, stage it for removal(done in last condition)
        // and remove the file from the working directory
        // if the user has not already done so
        // HACK: blob.exists maybe could without judgement
        if (blobId.equals(headBlobId) && blob.exists()) {
            // remove the file from the working directory
            restrictedDelete(file);
        }

        writeStage(stage);
    }

    public static void log() {
        StringBuffer sb = new StringBuffer();
        Commit commit = getHead();
        while (commit != null) {
            sb.append(commit.getCommitAsString());
            commit = getCommitFromId(commit.getFirstParentId());
        }

        System.out.print(sb);
    }

    public static void global_log() {
        StringBuffer sb = new StringBuffer();
        List<String> fileNames = plainFilenamesIn(COMMIT_DIR);
        for (String fileName : fileNames) {
            Commit commit = getCommitFromId(fileName);
            sb.append(commit.getCommitAsString());
        }
        System.out.println(sb);
    }

    public static void find(String msg) {
        StringBuffer sb = new StringBuffer();
        List<String> fileNames = plainFilenamesIn(COMMIT_DIR);
        for (String fileName: fileNames) {
            Commit commit = getCommitFromId(fileName);
            if (commit.getMessage().contains(msg)) {
                sb.append(commit.getMessage());
            }
        }
        if (sb.length() == 0) {
            exit("Found no commit with that message.");
        }
        System.out.println(sb);
    }

    public static void status() {
        StringBuffer sb = new StringBuffer();

        sb.append("=== Branches ===\n");
        String headBranch = readContentsAsString(HEAD);
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        for (String branch : branches) {
            if (branch.equals(headBranch)) {
                sb.append("*" + branch + "\n");
            } else {
                sb.append(branch + "\n");
            }
        }
        sb.append("\n");

        Stage stage = readStage();
        sb.append("=== Staged Files ===\n");
        for (String fileName : stage.getAdded().keySet()) {
            sb.append(fileName + "\n");
        }
        sb.append("\n");

        sb.append("=== Removed Files ===\n");
        for (String fileName : stage.getRemoved()) {
            sb.append(fileName + "\n");
        }
        sb.append("\n");

        // TODO: status ec
        sb.append("=== Modifications Not Staged For Commit ===\n");
        sb.append("\n");
        sb.append("=== Untracked Files ===\n");
        sb.append("\n");

        System.out.println(sb);
    } 

    /**
     * @param commit Commit Object which will be Serialized.
     */
    private static void writeCommitToFile(Commit commit) {
        File file = join(COMMIT_DIR, commit.getId()); // now, without Tries firstly...
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
        File file = join(COMMIT_DIR, commitId);
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

    private static void commitWith(String msg, List<Commit> parents) {
        Stage stage = readStage();
        // If no files have been staged, abort
        if (stage.isEmpty()) {
            exit("No changes added to the commit.");
        }

        Commit commit = new Commit(msg, parents, stage);
        // The staging area is cleared after a commit.
        clearStage(stage);
        writeCommitToFile(commit);

        updateBranch(commit);
    }

    /**
     * mv staging's blob to object.
     * @param stage
     */
    private static void clearStage(Stage stage) {
        File[] files = STAGING_DIR.listFiles();
        if (files == null) {
            return;
        }
        Path targetDir = BLOBS_DIR.toPath();
        for (File file: files) {
            Path source = file.toPath();
            try {
                Files.move(source, targetDir.resolve(source.getFileName()), REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // will cover stage.
        writeStage(new Stage());
    }

    private static void updateBranch(Commit commit) {
        String commitId = commit.getId();
        String branchName = getHeadBranchName();
        File branch = getBranchFile(branchName);
        writeContents(branch, commitId);
    }
}
