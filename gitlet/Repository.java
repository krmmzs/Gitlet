package gitlet;

import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FilenameFilter;
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
     * The stage Object.(replace index)
     */
    public static final File STAGE = join(GITLET_DIR, "stage");

    /**
     * The Objects directory, stores commits and blobs.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "Objects");

    /**
     * The staging directory, restores staging Blobs.
     */
    public static final File STAGING_DIR = join(OBJECTS_DIR, "staging");

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
        String id = initialCommit.getId();
        // String branchName = "master";
        File default_branch = join(HEADS_DIR, DEFAULT_BRANCH);
        writeContents(default_branch, id); // .gitlet/refs/heads/master
        writeContents(HEAD, DEFAULT_BRANCH); // .gitlet/HEAD
    }

    private static void createInitDir() {
        // Need to pay attention to the order
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        writeObject(STAGE, new Stage());
        STAGING_DIR.mkdir();
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
            exit("File does not exist.");
        }

        Blob blob = new Blob(fileName, CWD); // using file name to instance this blob.
        String blobId = blob.getId();

        // gettheHeadCommit
        Commit head = getHead();
        // get the Stage
        Stage stage = readStage();

        String headBlobId = head.getBlobs().getOrDefault(fileName, ""); // using file name to find file in current Commit.
        String stageBlobId = stage.getAdded().getOrDefault(fileName, ""); // usign file name to find file in stage.

        // the current working version of the file is identical to 
        // the version in the current commit do not stage it be added.
        // and remove it from the staging area if it is already there
        if (blobId.equals(headBlobId)) {
            // delete the file from staging
            join(STAGING_DIR, stageBlobId).delete();

            stage.getAdded().remove(fileName);
            stage.getRemoved().remove(fileName);
            writeStage(stage);
        } else if (!blobId.equals(stageBlobId)) {
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
        // get the current commit
        // get the stage
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
        // commit, stage it for removal(done in last condition, untracked means )
        // and remove the file from the working directory
        // if the user has not already done so
        if (blobId.equals(headBlobId)) {
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
            Commit commit = getCommitFromId(fileName); // at this time, file name == commit id.
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
                sb.append(commit.getId() + "\n");
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

    // checkoutBranch(args[1]);
    // Repository.checkoutFileFromHead(args[2]);
    // Repository.checkoutFileFromCommitId(args[3]);

    // Differences from real git: Real git does not clear
    // the staging area and stages the file that is checked out.
    // Also, it won’t do a checkout that would overwrite or undo
    // changes (additions or removals) that you have staged.

    /**
     * other's branchName -> branchFile -> ...
     * @param branchName
     */
    public static void checkoutBranch(String branchName) {
        File branchFile = getBranchFile(branchName);
        // There is no corresponding branch name
        // or no corresponding file.
        if (!branchFile.exists()) {
            exit("No such branch exists.");
        }

        String headBranchName = getHeadBranchName();
        // If that branch is the current branch,
        if (headBranchName.equals(branchName)) {
            exit("No need to checkout the current branch.");
        }

        // If a working file is untracked in the current
        // branch and would be overwritten by the checkout
        Commit otherCommit = getCommitFromBranchName(branchName);
        validUntrackedFile(otherCommit.getBlobs());

        // see Differences from real git
        clearStage(readStage());
        replaceWorkingPlaceWithCommit(otherCommit);

        // the given branch will now be considered the current branch (HEAD).
        writeContents(HEAD, branchName);
    }

    /**
     * HEAD -> commit -> (blobs -> blobId(need check exist) -> blob -> file -> writecontents)
     * @param branchName
     */
    public static void checkoutFileFromHead(String fileName) {
        Commit head = getHead();
        checkoutFileFromCommit(fileName, head);
    }

    /**
     * commitId -> commit File -> commit -> function:checkoutFileFromCommit
     * @param commitId
     * @param fileName
     */
    public static void checkoutFileFromCommitId(String commitId, String fileName) {
        commitId = getCompleteCommitId(commitId);
        File commitFile = join(COMMIT_DIR, commitId);
        if (!commitFile.exists()) {
            exit("No commit with that id exists.");
        }
        Commit commit = readObject(commitFile, Commit.class);
        checkoutFileFromCommit(fileName, commit);
    }

    /**
     * branchName -> branch file -> commitId -> writeContents
     * @param branchName
     */
    public static void branch(String  branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            exit("A branch with that name already exists.");
        }

        // points it at the current head commit.
        String commitId = getHeadCommitId();
        writeContents(branchFile, commitId);
    }

    /**
     * branchName -> branch file -> deleteFile
     * @param branchName
     */
    public static void rmBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            exit("A branch with that name does not exist.");
        }

        String headBranchName = getHeadBranchName();
        if (headBranchName.equals(branchName)) {
            exit("Cannot remove the current branch.");
        }

        branchFile.delete();
    }

    /**
     * <pre>
     * commitId -> commit file -> commit -> 
     * Checks out all the files tracked by the given commit
     *
     * @param commitId
     * <pre>
     */
    public static void reset(String commitId) {
        File file = join(COMMIT_DIR, commitId);
        if (!file.exists()) {
            exit("No commit with that id exists.");
        }

        Commit commit = getCommitFromId(commitId);

        // Failure case: If no commit with the given id exists
        validUntrackedFile(commit.getBlobs());

        replaceWorkingPlaceWithCommit(commit);
        clearStage(readStage());

        // Also moves the current branch’s head to that commit node.
        String headBranchName = getHeadBranchName();
        writeContents(join(HEADS_DIR, headBranchName), commitId);
    }

    /**
     * <pre>
     * 1. Modified in other but not HEAD -> other
     * 2. Modified in HEAD but not other -> HEAD
     * 3. Modified in other and HEAD 1. in same way -> DNM(same) 2. in diff ways -> conflct
     * 4. Not in split nor other but in HEAD -> HEAD
     * 5. Not in split nor HEAD but in other -> other
     * 6. Unmodified in HEAD but not present in other -> remove
     * 7. Unmodified in other but not present in HEAD -> remain remove
     * <pre>
     *
     * @param branchName
     */
    public static void merge(String otherBranchName) {
        // If there are staged additions or removals present,
        Stage stage = readStage();
        if (!stage.isEmpty()) {
            exit("You have uncommitted changes.");
        }

        // check the headBranchName and otherBranchFile
        File otherBranchFile = getBranchFile(otherBranchName);
        if (!otherBranchFile.exists()) {
            exit("A branch with that name does not exist.");
        }


        String headBranchName = getHeadBranchName();
        if (headBranchName.equals(otherBranchName)) {
            exit("Cannot merge a branch with itself.");
        }
        
        // bug free this line

        // get head commit and other commit
        Commit head = getCommitFromBranchName(headBranchName);
        Commit other = getCommitFromBranchFile(otherBranchFile);
        // get lca
        Commit lca = getLca(head, other);

        // If the split point is the same commit as the given branch, then we do nothing(don't exit)
        if (lca.getId().equals(other.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        // If the split point is the current branch, then the effect is to check out the given branch
        if (lca.getId().equals(head.getId())) {
            checkoutBranch(otherBranchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        mergeWithLca(lca, head, other);

        String msg = "Merged " + otherBranchName + " into " + headBranchName + ".";
        List<Commit> parents = List.of(head, other);
        commitWith(msg, parents);
    }

    /**
     * Get all the file and judging what needs to be done by the status of these files.
     *
     * @param lca
     * @param head
     * @param other
     */
    private static void mergeWithLca(Commit lca, Commit head, Commit other) {
        Set<String> fileNames = getAllFileName(lca, head, other);

        List<String> remove = new LinkedList<>();
        List<String> rewrite = new LinkedList<>();
        List<String> conflict = new LinkedList<>();

        for (String fileName : fileNames) {
            String lId = lca.getBlobs().getOrDefault(fileName, "");
            String hId = head.getBlobs().getOrDefault(fileName, "");
            String oId = other.getBlobs().getOrDefault(fileName, "");

            if (hId.equals(oId) || lId.equals(oId)) {
                continue;
            }
            if (lId.equals(hId)) {
                if (oId.equals("")) {
                    remove.add(fileName);
                }
                else {
                    // Any files that were not present at the split
                    // point and are present only in the given branch
                    // should be checked out and staged.
                    rewrite.add(fileName);
                }
            } else {
                conflict.add(fileName);
            }
        }

        // If an untracked file in the current commit would be overwritten or deleted by the merge,
        List<String> untrackedFiles = getUntrackedFiles();
        for (String fileName : untrackedFiles) {
            if (remove.contains(fileName) || rewrite.contains(fileName) || conflict.contains(fileName)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }

        if (!remove.isEmpty()) {
            for (String fileName : remove) {
                rm(fileName);
            }
        }

        // fileName -> blobId -> blob -> blobFile -> write
        if (!rewrite.isEmpty()) {
            // Any files that were not present at the split
            // point and are present only in the given branch
            // should be checked out and staged.
            for (String fileName : rewrite) {
                String oId = other.getBlobs().getOrDefault(fileName, "");
                Blob blob = getBlobFromId(oId);
                checkoutFileFromBlob(blob);
                // add the file.
                add(fileName);
            }
        }

        if (!conflict.isEmpty()) {
            for (String fileName : conflict) {
                String hId = head.getBlobs().getOrDefault(fileName, "");
                String oId = other.getBlobs().getOrDefault(fileName, "");

                String headContent = getContentAsStringFromBlobId(hId);
                String otherContent = getContentAsStringFromBlobId(oId);
                String content = getConflictFile(headContent.split("\n"), otherContent.split("\n"));
                rewriteFile(fileName, content);
                System.out.println("Encountered a merge conflict.");
            }
        }

    }

    private static void rewriteFile(String fileName, String content) {
        File file = join(CWD, fileName);
        writeContents(file, content);
    }

    private static String getConflictFile(String[] head, String[] other) {
        StringBuffer sb = new StringBuffer();
        int len1 = head.length, len2 = other.length;
        int i = 0, j = 0;
        while (i < len1 && j < len2) {
            if (head[i].equals(other[i])) {
                sb.append(head[i]);
            } else {
                sb.append(getConflictContent(head[i], other[i]));
            }
            i += 1;
            j += 1;
        }

        while (i < len1) {
            sb.append(getConflictContent(head[i], ""));
            i += 1;
        }
        while (j < len2) {
            sb.append(getConflictContent("", other[i]));
            j += 1;
        }
        return sb.toString();
    }

    private static String getConflictContent(String head, String other) {
        StringBuffer sb = new StringBuffer();
        sb.append("<<<<<<< HEAD\n");
        sb.append(head.equals("") ? head : head + "\n");
        sb.append("=======\n");
        sb.append(other.equals("") ? other : other + "\n");
        sb.append(">>>>>>>\n");
        return sb.toString();
    }

    private static String getContentAsStringFromBlobId(String blobId) {
        if (blobId.equals("")) {
            return "";
        }
        return getBlobFromId(blobId).getContentAsString();
    }

    /**
     * get all commits's files.
     * @param lca
     * @param head
     * @param other
     * @return
     */
    private static Set<String> getAllFileName(Commit lca, Commit head, Commit other) {
        Set<String> set = new HashSet<>();
        set.addAll(lca.getBlobs().keySet());
        set.addAll(head.getBlobs().keySet());
        set.addAll(other.getBlobs().keySet());

        return set;
    }

    /**
     * @param head
     * @param other
     * @return
     */
    private static Commit getLca(Commit head, Commit other) {
        // get the headAncestors using bfs
        Set<String> headAncestors = bfsFromCommit(head);
        Queue<Commit> queue = new LinkedList<>();
        queue.add(other);

        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            if (headAncestors.contains(commit.getId())) {
                return commit;
            }
            if (!commit.getParents().isEmpty()) {
                for (String id : commit.getParents()) {
                    queue.add(getCommitFromId(id));
                }
            }
        }
        return new Commit();
    }

    private static Set<String> bfsFromCommit(Commit head) {
        Set<String> res = new HashSet<>();
        Queue<Commit> queue = new LinkedList<>();
        queue.add(head);
        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            if (!res.contains(commit.getId()) && !commit.getParents().isEmpty()) {
                for (String id : commit.getParents()) {
                    queue.add(getCommitFromId(id));
                }
            }
            res.add(commit.getId());
        }

        return res;
    }

    /**
     * HEAD -> branchName -> ranchFile -> readContentsAsString
     * @return
     */
    private static String getHeadCommitId() {
        String branchName = getHeadBranchName();
        File branchFile = getBranchFile(branchName);
        return readContentsAsString(branchFile);
    }

    private static String getCompleteCommitId(String commitId) {
        if (commitId.length() == UID_LENGTH) {
            return commitId;
        }

        for (String fileName : COMMIT_DIR.list()) {
            if (fileName.startsWith(commitId)) {
                return fileName;
            }
        }
        return null;
    }

    /**
     * blobs -> (blobId(need check exist) -> blob -> file -> writecontents)
     * @param commit
     * @param fileName
     */
    private static void checkoutFileFromCommit(String fileName, Commit commit) {
        String  blobId = commit.getBlobs().getOrDefault(fileName, "");
        checkoutFileFromBlobId(blobId);
    }

    /**
     * blobId(need check exist) -> (blob -> file -> writecontents)
     * @param blobId
     */
    private static void checkoutFileFromBlobId(String blobId) {
        if (blobId.equals("")) {
            exit("File does not exist in that commit.");
        }
        Blob blob = getBlobFromId(blobId);
        checkoutFileFromBlob(blob);
    }

    private static void checkoutFileFromBlob(Blob blob) {
        File file = join(CWD, blob.getFileName());
        writeContents(file, blob.getContent());
    }

    private static Blob getBlobFromId(String blobId) {
        File file = join(BLOBS_DIR, blobId);
        return readObject(file, Blob.class);
    }

    /**
     * @param commit Commit Object which will be Serialized.
     */
    private static void writeCommitToFile(Commit commit) {
        File file = join(COMMIT_DIR, commit.getId()); // now, without Tries firstly...
        writeObject(file, commit);
    }

    private static Commit getHead() {
        String branchName = getHeadBranchName();
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

    /**
     * branchFile -> commitId -> commit
     * @param file
     * @return
     */
    private static Commit getCommitFromBranchFile(File file) {
        String commitId = readContentsAsString(file);
        return getCommitFromId(commitId);
    }

    /**
     * branchName -> branchFile -> commitId -> commit
     * @param branchName
     * @return
     */
    private static Commit getCommitFromBranchName(String branchName) {
        File file = getBranchFile(branchName);
        return getCommitFromBranchFile(file);
    }


    private static String getHeadBranchName() {
        return readContentsAsString(HEAD);
    }

    private static Commit getCommitFromId(String commitId) {
        File file = join(COMMIT_DIR, commitId);
        // original: commitId.equals("null") ...
        if (commitId.equals("") || !file.exists()) {
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

    /**
     * @param blobId for file Name
     * @param blob for file Contents
     */
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

    /**
     * If a working file is untracked in the current branch
     * and would be overwritten by the blobs(checkout).
     */
    private static void validUntrackedFile(Map<String, String> blobs) {
        List<String> untrackedFiles = getUntrackedFiles(); // get CWD's untracked files
        if (untrackedFiles.isEmpty()) {
            return;
        }

        for (String fileName : untrackedFiles) {
            String blobId = new Blob(fileName, CWD).getId();
            String otherId = blobs.getOrDefault(fileName, "");
            if (!otherId.equals(blobId)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /** 
     * Untracked basically means that Git sees a file you didn't
     * have in the previous snapshot (commit), and which hasn't
     * yet been staged;
     *
     * @return Untracked Files's Name
     */
    private static List<String> getUntrackedFiles() {
        List<String> res = new ArrayList<>();
        List<String> stageFiles = readStage().getStagedFileName();
        Set<String> headFiles = getHead().getBlobs().keySet();
        for (String fileName : plainFilenamesIn(CWD)) {
            if (!stageFiles.contains(fileName) && !headFiles.contains(fileName)) {
                res.add(fileName);
            }
        }
        Collections.sort(res);
        return res;
    }

    private static void replaceWorkingPlaceWithCommit(Commit commit) {
        clearWoringSpace();

        for (Map.Entry<String, String> entry : commit.getBlobs().entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue();
            Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);

            File file = join(CWD, fileName);
            writeContents(file, blob.getContent());
        }
    }

    private static void clearWoringSpace() {
        File[] files = CWD.listFiles(gitletFliter);
        for (File file : files) {
            delFile(file);
        }
    }

    /**
     * Delete files recursively
     * @param file
     */
    private static void delFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delFile(f);
            }
        }
        file.delete();
    }

    private static FilenameFilter gitletFliter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return !name.equals(".gitlet");
        }
    };
}
