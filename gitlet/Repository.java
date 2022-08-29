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
    private File CWD;
    /**
     * The .gitlet directory.(worktree/.git).
     */
    private File GITLET_DIR;


    /** 
     * The stage Object.(replace index)
     */
    private File STAGE;

    /**
     * The Objects directory, stores commits and blobs.
     */
    private File OBJECTS_DIR;

    /**
     * The staging directory, restores staging Blobs.
     */
    private File STAGING_DIR;

    /**
     * The Objects directory, stores committed blobs.
     */
    private File BLOBS_DIR;
    /**
     * The commits directory.
     */
    private File COMMIT_DIR;

    // The branches directory(Mimicking .git).

    /**
     * The reference directory.
     */
    private File REFS_DIR;
    /**
     * The heads directory.
     */
    private File HEADS_DIR;
    /**
     * The remotes directory.
     */
    private File REMOTES_DIR;

    /**
     * stores current branch's name if it points to tip
     */
    private File HEAD;
    // Note that in Gitlet, there is no way to be in a detached head state

    private File CONFIG;

    private String DEFAULT_BRANCH;

    /**
     * Lazy load for the current branch name.
     */
    private final Lazy<String> headBranchName = lazy(() -> {
        String headBranchName = readContentsAsString(HEAD);
        return headBranchName;
    });

    private final Lazy<Commit> head = lazy(() -> getHead());

    private final Lazy<Stage> stage = lazy(() -> readStage());

    public Repository() {
        this.CWD = new File(System.getProperty("user.dir"));
        configDIRS();
    }

    public Repository(String cwd) {
        this.CWD = new File(cwd);
        configDIRS();
    }

    private void configDIRS() {
        this.GITLET_DIR = join(CWD, ".gitlet");
        this.STAGE = join(GITLET_DIR, "stage");
        this.OBJECTS_DIR = join(GITLET_DIR, "Objects");
        this.STAGING_DIR = join(OBJECTS_DIR, "staging");
        this.BLOBS_DIR = join(OBJECTS_DIR, "blobs");
        this.COMMIT_DIR = join(OBJECTS_DIR, "commits");
        this.REFS_DIR = join(GITLET_DIR, "refs");
        this.HEADS_DIR = join(REFS_DIR, "heads");
        this.REMOTES_DIR = join(REFS_DIR, "remotes");
        this.HEAD = join(GITLET_DIR, "HEAD");
        this.CONFIG = join(GITLET_DIR, "config");
        this.DEFAULT_BRANCH = "master";
    }

    public void init() {
        if (GITLET_DIR.exists() && GITLET_DIR.isDirectory()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
        // create directory (.gitlet)
        createInitDir();
        // inital commit
        Commit initialCommit = new Commit();
        // initialCommit.saveCommit();
        writeCommitToFile(initialCommit);
        initReference(initialCommit.getId());
        // init config file required firsttly.
        createConfigFile();
    }


    private void createInitDir() {
        // Need to pay attention to the order
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        STAGING_DIR.mkdir();
        BLOBS_DIR.mkdir();
        COMMIT_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();
        REMOTES_DIR.mkdir();
        createStage();
    }

    public void checkInit() {
        if (!GITLET_DIR.isDirectory()) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /** 
     * <pre>
     * 1. staging the file for addition
     * (adding a file is also called staging the file for addition.)
     * 2. If the current working version of the file is identical
     * to the version in the current commit, do not stage it to be added
     * and remove it from the staging area if it is already there(
     * as can happen when a file is changed, added, and then changed
     * back to it’s original version)
     * <pre>
     * @param fileName added file name.
     */
    public void add(String fileName) {
        // get file from CWD
        File file = join(CWD, fileName);
        if (!file.exists()) {
            exit("File does not exist.");
        }

        Blob cwdBlob = new Blob(fileName, CWD); // using file name to instance this blob.
        String cwdBlobId = cwdBlob.getId();

        // gettheHeadCommit
        // using file name to find file in current Commit.
        String headBlobId = head.get().getBlobs().getOrDefault(fileName, "");
        // usign file name to find file in stage.
        String stageBlobId = stage.get().getAdded().getOrDefault(fileName, "");

        // the current working version of the file is identical to 
        // the version in the current commit do not stage it be added
        // and remove it from the staging area if it is already there.
        if (cwdBlobId.equals(headBlobId)) {
            // delete the file from staging
            join(STAGING_DIR, stageBlobId).delete();

            stage.get().getAdded().remove(fileName);
            stage.get().getRemoved().remove(fileName);
            writeStage(stage.get());
        } else if (!cwdBlobId.equals(stageBlobId)) {
            // update new version
            writeBlobToStaging(cwdBlobId, cwdBlob);
            updateStageAddedBlob(fileName, cwdBlobId);
        }
    }

    public void commit(String msg) {
        if (msg.equals("")) {
            exit("Please enter a commit message.");
        }
        commitWith(msg, List.of(head.get()));
    }

    public void rm(String fileName) {
        String headBlobId = head.get().getBlobs().getOrDefault(fileName, "");
        String stageBlobId = stage.get().getAdded().getOrDefault(fileName, "");
        if (headBlobId.equals("") && stageBlobId.equals("")) {
            exit("No reason to remove the file.");
        }

        // Unstage the file if it is currently staged for addition.
        if (!stageBlobId.equals("")) {
            unstageBlob(fileName);
        } else {
            stageForRemoved(fileName);
        }

        Blob cwdBlob = new Blob(fileName, CWD);
        String cwdBlobId = cwdBlob.getId();
        File file = join(CWD, fileName);
        // If the file is tracked in the current
        // commit, stage it for removal(done in last condition, untracked means)
        // and remove the file from the working directory
        // if the user has not already done so
        if (cwdBlobId.equals(headBlobId)) {
            // remove the file from the working directory
            restrictedDelete(file);
        }

        writeStage(stage.get());
    }

    public void log() {
        StringBuffer sb = logIteratorFromHead(head.get());
        System.out.print(sb);
    }

    public void globalLog() {
        StringBuffer sb = logAllCommit();
        System.out.println(sb);
    }

    public void find(String msg) {
        StringBuffer sb = getCommitAsStringByMsg(msg);
        if (sb.length() == 0) {
            exit("Found no commit with that message.");
        }
        System.out.println(sb);
    }

    private StringBuffer getCommitAsStringByMsg(String msg) {
        StringBuffer sb = new StringBuffer();
        List<String> commitsFileNames = plainFilenamesIn(COMMIT_DIR);
        for (String fileName: commitsFileNames) {
            Commit commit = getCommitFromId(fileName);
            if (commit.getMessage().contains(msg)) {
                sb.append(commit.getId() + "\n");
            }
        }
        return sb;
    }

    public void status() {
        StringBuffer sb = new StringBuffer();

        sb.append("=== Branches ===\n");
        appendBranch(sb);
        sb.append("\n");

        sb.append("=== Staged Files ===\n");
        appendStagedFiles(sb);
        sb.append("\n");

        sb.append("=== Removed Files ===\n");
        appendRemovedFiles(sb);
        sb.append("\n");

        sb.append("=== Modifications Not Staged For Commit ===\n");
        appenendMNSFC(sb);
        sb.append("\n");

        sb.append("=== Untracked Files ===\n");
        appendUntrackedFiles(sb);
        sb.append("\n");

        System.out.println(sb);
    }


    // Differences from real git: Real git does not clear
    // the staging area and stages the file that is checked out.
    // Also, it won’t do a checkout that would overwrite or undo
    // changes (additions or removals) that you have staged.

    /**
     * other's branchName -> branchFile -> ...
     * @param branchName
     */
    public void checkoutBranch(String branchName) {
        File branchFile = getBranchFile(branchName);
        // There is no corresponding branch name
        // or no corresponding file.
        if (!branchFile.exists()) {
            exit("No such branch exists.");
        }

        // If that branch is the current branch,
        if (headBranchName.get().equals(branchName)) {
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
        writeHEAD(branchName);
    }

    /**
     * HEAD -> commit -> (blobs -> blobId(need check exist) -> blob -> file -> writecontents)
     * @param branchName
     */
    public void checkoutFileFromHead(String fileName) {
        checkoutFileFromCommit(fileName, head.get());
    }

    /**
     * commitId -> commit File -> commit -> function:checkoutFileFromCommit
     * @param commitId
     * @param fileName
     */
    public void checkoutFileFromCommitId(String commitId, String fileName) {
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
    public void branch(String  branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            exit("A branch with that name already exists.");
        }

        // points it at the current head commit.
        String commitId = getHeadCommitId();
        writeBranch(branchFile, commitId);
    }

    /**
     * branchName -> branch file -> deleteFile
     * @param branchName
     */
    public void rmBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            exit("A branch with that name does not exist.");
        }

        if (headBranchName.get().equals(branchName)) {
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
    public void reset(String commitId) {
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
        writeBranch(join(HEADS_DIR, headBranchName.get()), commitId);
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
    public void merge(String otherBranchName) {
        // If there are staged additions or removals present,
        if (!stage.get().isEmpty()) {
            exit("You have uncommitted changes.");
        }

        // check the headBranchName and otherBranchFile
        File otherBranchFile = getBranchFile(otherBranchName);
        if (!otherBranchFile.exists()) {
            exit("A branch with that name does not exist.");
        }

        if (headBranchName.get().equals(otherBranchName)) {
            exit("Cannot merge a branch with itself.");
        }

        // bug free this line

        // get head commit and other commit
        // Commit head = getCommitFromBranchName(headBranchName);
        Commit other = getCommitFromBranchFile(otherBranchFile);
        // get lca
        Commit lca = getLca(head.get(), other);

        // If the split point is the same commit as the given branch, then we do nothing(don't exit)
        if (lca.getId().equals(other.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        // If the split point is the current branch,
        // then the effect is to check out the given branch
        if (lca.getId().equals(head.get().getId())) {
            checkoutBranch(otherBranchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        mergeWithLca(lca, head.get(), other);

        String msg = "Merged " + otherBranchName + " into " + headBranchName.get() + ".";
        List<Commit> parents = List.of(head.get(), other);
        commitWith(msg, parents);
    }

    /**
     * <pre>
     * java gitlet.Main add-remote [remote name] [name of remote directory]/.gitlet
     * Saves the given login information under the given remote name
     * <pre>
     * @param remoteName
     * @param remotePath
     */
    public void addRemote(String remoteName,  String remotePath) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (remoteFile.exists()) {
            exit("A remote with that name already exists.");
        }
        remoteFile.mkdir();

        // java.io.File.separator
        if (File.separator.equals("\\")) {
            remotePath = remotePath.replaceAll("/", "\\\\\\\\");
        }

        /*
         * same as git
        [remote "origin"]
            url = ..\\remotegit\\.git
            fetch = +refs/heads/*:refs/remotes/origin/*
         */
        String contents = readContentsAsString(CONFIG);
        contents += "[remote \"" + remoteName + "\"]\n";
        contents += remotePath + "\n";

        writeContents(CONFIG, contents);
    }

    /**
     * java gitlet.Main rm-remote [remote name]
     * Remove information associated with the given remote name.
     * @param remoteName
     */
    public void rmRemote(String remoteName) {
        File remote = join(REMOTES_DIR, remoteName);
        if (!remote.exists()) {
            exit("A remote with that name does not exist.");
        }

        delFileRec(remote);

        String[] contents = readContentsAsString(CONFIG).split("\n");
        String target = "[remote \"" + remoteName + "\"]";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < contents.length;) {
            if (contents[i].equals(target)) {
                // skip content
                i += 2;
            } else {
                sb.append(contents[i]);
            }
        }
        writeContents(CONFIG, sb.toString());
    }

    /**
     * remoteName -> remotePath.
     * Attempts to append the current branch’s commits to
     * the end of the given branch at the given remote. 
     * @param remoteName
     * @param branchName
     */
    public void push(String remoteName, String remoteBranchName) {
        File remotePath = getRemotePath(remoteName);
        Repository remote = new Repository(remotePath.getParent());

        Commit remoteHead = remote.getHead();
        List<String> history = getHistory(head.get());
        // If the remote branch’s head is not in the
        // history of the current local head.
        if (!history.contains(remoteHead.getId())) {
            exit("Please pull down remote changes before pushing.");
        }

        // If the Gitlet system on the remote machine exists but does not
        // have the input branch, then simply add the branch to the remote Gitlet.
        File remoteBranch = join(remote.HEADS_DIR, remoteBranchName);
        if (!remoteBranch.exists()) {
            remote.branch(remoteBranchName);
        }

        // append the future commits to the remote branch.
        for (String commitId : history) {
            // until the end of the given branch at the given remote.
            if (commitId.equals(remoteHead.getId())) {
                break;
            }
            Commit commit = getCommitFromId(commitId);
            // cp commit's persisting contents.
            File remoteCommit = join(remote.COMMIT_DIR, commitId);
            writeObject(remoteCommit, commit);

            // cp commit's blobs's persisting contents to remote.
            if (!commit.getBlobs().isEmpty()) {
                for (Map.Entry<String, String> entry : commit.getBlobs().entrySet()) {
                    String blobId = entry.getValue();
                    Blob blob = getBlobFromId(blobId);

                    File remoteBlob = join(remote.BLOBS_DIR, blobId);
                    writeObject(remoteBlob, blob);
                }
            }
        }

        // Then, the remote should reset to the front of
        // the appended commits.
        remote.reset(head.get().getId());
    }

    /**
     * Brings down commits from the remote Gitlet repository
     * into the local Gitlet repository.
     * @param remoteName
     * @param remoteBranchName
     */
    public void fetch(String remoteName, String remoteBranchName) {
        File remotePath = getRemotePath(remoteName);

        Repository remote = new Repository(remotePath.getParent());

        File remoteBranchFile = remote.getBranchFile(remoteBranchName);
        if (remoteBranchFile == null || !remoteBranchFile.exists()) {
            exit("That remote does not have that branch.");
        }

        Commit remoteBranchCommit = remote.getCommitFromBranchFile(remoteBranchFile);
        // This branch is created in the local repository
        // if it did not previously exist.
        File branch = join(REMOTES_DIR, remoteName, remoteBranchName);
        writeContents(branch, remoteBranchCommit.getId());

        // copies all commits and blobs from the given
        // branch in the remote repository.
        List<String> history = remote.getHistory(remoteBranchCommit);

        for (String commitId : history) {
            Commit commit = remote.getCommitFromId(commitId);
            File commitFile = join(COMMIT_DIR, commit.getId());
            if (commitFile.exists()) {
                continue;
            }
            writeObject(commitFile, commit);

            if (commit.getBlobs().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> entry : commit.getBlobs().entrySet()) {
                String blobId = entry.getValue();
                Blob blob = remote.getBlobFromId(blobId);

                File blobFile = join(BLOBS_DIR, blobId);
                writeObject(blobFile, blob);
            }
        }
    }

    /**
     * Fetches branch [remote name]/[remote branch name] 
     * as for the fetch command, and then merges that fetch into the current branch.
     * @param remoteName
     * @param remoteBranchName
     */
    public void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);

        String otherBranchName = remoteName + "/" + remoteBranchName;
        merge(otherBranchName);
    }

    private void writeBranch(File branchFile, String commitId) {
        writeContents(branchFile, commitId);
    }

    /**
     * write branch name to HEAD.
     * @param branchName
     */
    private void writeHEAD(String branchName) {
        writeContents(HEAD, branchName);
    }


    private void appendUntrackedFiles(StringBuffer sb) {
        List<String> untrackedFiles = getUntrackedFiles();
        for (String filename : untrackedFiles) {
            sb.append(filename + "\n");
        }
    }

    private void appenendMNSFC(StringBuffer sb) {
        List<String> modifiedFiles = getModifiedFiles(getCommitFromBranchName(headBranchName.get()), stage.get());
        for (String str : modifiedFiles) {
            sb.append(str + "\n");
        }
    }

    private void appendRemovedFiles(StringBuffer sb) {
        for (String fileName : stage.get().getRemoved()) {
            sb.append(fileName + "\n");
        }
    }

    private void appendStagedFiles(StringBuffer sb) {
        for (String fileName : stage.get().getAdded().keySet()) {
            sb.append(fileName + "\n");
        }
    }

    private void appendBranch(StringBuffer sb) {
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        for (String branch : branches) {
            if (branch.equals(headBranchName.get())) {
                sb.append("*" + branch + "\n");
            } else {
                sb.append(branch + "\n");
            }
        }
    }

    /**
     * log all commit file as string.
     * @return String Buffer
     */
    private StringBuffer logAllCommit() {
        StringBuffer sb = new StringBuffer();
        List<String> commitsFileNames = plainFilenamesIn(COMMIT_DIR);
        for (String fileName : commitsFileNames) {
            // at this time, file name == commit id.
            Commit commit = getCommitFromId(fileName); 
            sb.append(commit.getCommitAsString());
        }
        return sb;
    }

    /**
     * <pre>
     * Get the commit String format from head to init commit 
     * by Iterator help function.
     * <pre>
     * @param p
     * @return
     */
    private StringBuffer logIteratorFromHead(Commit p) {
        StringBuffer sb = new StringBuffer();
        while (p != null) {
            sb.append(p.getCommitAsString());
            p = getCommitFromId(p.getFirstParentId());
        }
        return sb;
    }


    private void stageForRemoved(String fileName) {
        stage.get().getRemoved().add(fileName);
    }

    private void unstageBlob(String fileName) {
        stage.get().getAdded().remove(fileName);
    }


    /**
     * Init a config file.
     */
    private void createConfigFile() {
        writeContents(CONFIG, "");
    }

    /**
     * update Stage's added(file name -> id) and serialize stage.
     * @param fileName
     * @param cwdBlobId
     */
    private void updateStageAddedBlob(String fileName, String cwdBlobId) {
        stage.get().add(fileName, cwdBlobId);
        writeStage(stage.get());
    }


    /**
     * create Master and HEAD
     * @param initialCommit
     */
    private void initReference(String id) {
        File defaultBranchFile = join(HEADS_DIR, DEFAULT_BRANCH);
        writeContents(defaultBranchFile, id); // .gitlet/refs/heads/master(defalut)
        writeHEAD(DEFAULT_BRANCH); // .gitlet/HEAD
    }

    private void createStage() {
        writeObject(STAGE, new Stage());
    }

    /**
     * bfs get the history commits.
     * @param head
     * @return
     */
    private List<String> getHistory(Commit head) {
        List<String> res = new LinkedList<>();
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

    private File getRemotePath(String remoteName) {
        String path = "";
        String[] contents = readContentsAsString(CONFIG).split("\n");
        for (int i = 0; i < contents.length;) {
            if (contents[i].contains(remoteName)) {
                path = contents[i + 1];
                break;
            } else {
                i += 2;
            }
        }

        File file = null;
        try {
            file = new File(path).getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (path.equals("") || !file.exists()) {
            exit("Remote directory not found.");
        }
        return file;
    }

    private List<String> getModifiedFiles(Commit head, Stage stage) {
        List<String> res = new LinkedList<>();

        List<String> currentFiles = plainFilenamesIn(CWD);
        for (String fileName : currentFiles) {
            Blob blob = new Blob(fileName, CWD);
            // case1: Tracked in the current commit, changed in the working directory, but not
            // staged; or
            boolean tracked = head.getBlobs().containsKey(fileName);
            boolean changed = !blob.getId().equals(head.getBlobs().get(fileName));
            boolean staged = stage.getAdded().containsKey(fileName);
            if (tracked && changed && !staged) {
                res.add(fileName + " (modified)");
                continue;
            }
            // case2: Staged for addition, but with different contents than in the working
            // directory; or
            changed = !blob.getId().equals(stage.getAdded().get(fileName));
            if (staged && changed) {
                res.add(fileName + " (modified)");
            }
        }
        // case3: Staged for addition, but deleted in the working directory; or
        for (String fileName : stage.getAdded().keySet()) {
            if (!currentFiles.contains(fileName)) {
                res.add(fileName + " (deleted)");
            }
        }
        // case4: Not staged for removal, but tracked in the current commit and deleted from the
        // working directory.
        for (String fileName : head.getBlobs().keySet()) {
            boolean stagedForRemoval = stage.getRemoved().contains(fileName);
            boolean cwdContains = currentFiles.contains(fileName);
            if (!stagedForRemoval && !cwdContains) {
                res.add(fileName + " (deleted)");
            }
        }
        Collections.sort(res);
        return res;


        // Set<String> headFiles = head.getBlobs().keySet();
        // List<String> stagedFiles = stage.getStagedFilename();
        //
        // Set<String> allFiles = new HashSet<>();
        // allFiles.addAll(currentFiles);
        // allFiles.addAll(headFiles);
        // allFiles.addAll(stagedFiles);
        //
        // for (String filename : allFiles) {
        //     if (!currentFiles.contains(filename)) {
        //         if (stage.getAdded().containsKey(filename) ||
        //            (headFiles.contains(filename) && !stagedFiles.contains(filename))) {
        //             res.add(filename + " (deleted)");
        //         }
        //     } else {
        //         String bId = new Blob(filename, CWD).getId();
        //         String sId = stage.getAdded().getOrDefault(filename, "");
        //         String hId = head.getBlobs().getOrDefault(filename, "");
        //         if ((hId != "" && hId != bId && sId == "") ||
        //             (sId != "" && sId != bId)){
        //             res.add(filename + " (modified)");
        //         }
        //     }
        // }
        //
        // Collections.sort(res);
        // return res;
    }

    /**
     * Get all the file and judging what needs to be done by the status of these files.
     *
     * @param lca
     * @param head
     * @param other
     */
    private void mergeWithLca(Commit lca, Commit head, Commit other) {
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
                } else {
                    // Any files that were not present at the split
                    // point and are present only in the given branch
                    // should be checked out and staged.
                    rewrite.add(fileName);
                }
            } else {
                conflict.add(fileName);
            }
        }

        // If an untracked file in the current commit would
        // be overwritten or deleted by the merge,
        List<String> untrackedFiles = getUntrackedFiles();
        for (String fileName : untrackedFiles) {
            if (remove.contains(fileName) || rewrite.contains(fileName)
            || conflict.contains(fileName)) {
                exit("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
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
                // rewrite file and and stage the result.
                rewriteFile(fileName, content);
                add(fileName);
                System.out.println("Encountered a merge conflict.");
            }
        }

    }

    private void rewriteFile(String fileName, String content) {
        File file = join(CWD, fileName);
        writeContents(file, content);
    }

    private String getConflictFile(String[] head, String[] other) {
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

    private String getConflictContent(String head, String other) {
        StringBuffer sb = new StringBuffer();
        sb.append("<<<<<<< HEAD\n");
        sb.append(head.equals("") ? head : head + "\n");
        sb.append("=======\n");
        sb.append(other.equals("") ? other : other + "\n");
        sb.append(">>>>>>>\n");
        return sb.toString();
    }

    private String getContentAsStringFromBlobId(String blobId) {
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
    private Set<String> getAllFileName(Commit lca, Commit head, Commit other) {
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
    private Commit getLca(Commit head, Commit other) {
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

    private Set<String> bfsFromCommit(Commit head) {
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
    private String getHeadCommitId() {
        String branchName = getHeadBranchName();
        File branchFile = getBranchFile(branchName);
        return readContentsAsString(branchFile);
    }

    private String getCompleteCommitId(String commitId) {
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
    private void checkoutFileFromCommit(String fileName, Commit commit) {
        String  blobId = commit.getBlobs().getOrDefault(fileName, "");
        checkoutFileFromBlobId(blobId);
    }

    /**
     * blobId(need check exist) -> (blob -> file -> writecontents)
     * @param blobId
     */
    private void checkoutFileFromBlobId(String blobId) {
        if (blobId.equals("")) {
            exit("File does not exist in that commit.");
        }
        Blob blob = getBlobFromId(blobId);
        checkoutFileFromBlob(blob);
    }

    private void checkoutFileFromBlob(Blob blob) {
        File file = join(CWD, blob.getFileName());
        writeContents(file, blob.getContent());
    }

    private Blob getBlobFromId(String blobId) {
        File file = join(BLOBS_DIR, blobId);
        return readObject(file, Blob.class);
    }

    // TODO: with directory tries abs.
    /**
     * @param commit Commit Object which will be Serialized.
     */
    private void writeCommitToFile(Commit commit) {
        File file = join(COMMIT_DIR, commit.getId()); // now, without Tries firstly...
        writeObject(file, commit);
    }

    private Commit getHead() {
        String branchName = getHeadBranchName();
        File branchFile = getBranchFile(branchName);
        Commit head = getCommitFromBranchFile(branchFile);

        if (head == null) {
            exit("error: can't find this branch");
        }

        return head;
    }

    private File getBranchFile(String branchName) {
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
    private Commit getCommitFromBranchFile(File file) {
        String commitId = readContentsAsString(file);
        return getCommitFromId(commitId);
    }

    /**
     * branchName -> branchFile -> commitId -> commit
     * @param branchName
     * @return
     */
    private Commit getCommitFromBranchName(String branchName) {
        File file = getBranchFile(branchName);
        return getCommitFromBranchFile(file);
    }


    private String getHeadBranchName() {
        return readContentsAsString(HEAD);
    }

    private Commit getCommitFromId(String commitId) {
        File file = join(COMMIT_DIR, commitId);
        // original: commitId.equals("null") ...
        if (commitId.equals("") || !file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    } 

    private Stage readStage() {
        return readObject(STAGE, Stage.class);
    }

    private void writeStage(Stage stage) {
        writeObject(STAGE, stage);
    }

    /**
     * @param blobId for file Name
     * @param blob for file Contents
     */
    private void writeBlobToStaging(String blobId, Blob blob) {
        writeObject(join(STAGING_DIR, blobId), blob);
    }

    private void commitWith(String msg, List<Commit> parents) {
        // If no files have been staged, abort
        if (stage.get().isEmpty()) {
            exit("No changes added to the commit.");
        }

        Commit commit = new Commit(msg, parents, stage.get());
        // The staging area is cleared after a commit.
        clearStage(stage.get());
        writeCommitToFile(commit);

        updateBranch(commit);
    }

    /**
     * mv staging's blob to object.
     * @param stage
     */
    private void clearStage(Stage stage) {
        File[] files = STAGING_DIR.listFiles(File::isFile);
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

    private void updateBranch(Commit commit) {
        String commitId = commit.getId();
        String branchName = getHeadBranchName();
        File branch = getBranchFile(branchName);
        writeContents(branch, commitId);
    }

    /**
     * If a working file is untracked in the current branch
     * and would be overwritten by the blobs(checkout).
     */
    private void validUntrackedFile(Map<String, String> blobs) {
        List<String> untrackedFiles = getUntrackedFiles(); // get CWD's untracked files
        if (untrackedFiles.isEmpty()) {
            return;
        }

        for (String fileName : untrackedFiles) {
            String blobId = new Blob(fileName, CWD).getId();
            String otherId = blobs.getOrDefault(fileName, "");
            if (!otherId.equals(blobId)) {
                exit("There is an untracked file in the way; delete it,"
                    + " or add and commit it first.");
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
    private List<String> getUntrackedFiles() {
        List<String> res = new ArrayList<>();
        List<String> cwdFileNames = plainFilenamesIn(CWD);
        for (String fileName : cwdFileNames) {
            boolean tracked = head.get().getBlobs().containsKey(fileName);
            boolean staged = stage.get().getAdded().containsKey(fileName);
            // untracked files
            if (!staged && !tracked) {
                res.add(fileName);
            }
        }
        Collections.sort(res);
        return res;
    }

    private void replaceWorkingPlaceWithCommit(Commit commit) {
        clearWoringSpace();

        for (Map.Entry<String, String> entry : commit.getBlobs().entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue();
            Blob blob = readObject(join(BLOBS_DIR, blobId), Blob.class);

            File file = join(CWD, fileName);
            writeContents(file, blob.getContent());
        }
    }

    private void clearWoringSpace() {
        File[] files = CWD.listFiles(gitletFliter);
        for (File file : files) {
            delFileRec(file);
        }
    }

    /**
     * Delete files recursively
     * @param file
     */
    private void delFileRec(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                delFileRec(f);
            }
        }
        file.delete();
    }

    private FilenameFilter gitletFliter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return !name.equals(".gitlet");
        }
    };
}
