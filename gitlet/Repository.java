package gitlet;

import java.io.File;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *
 *  @author krmmzs
 */
public class Repository {

    /** The current working directory(work tree). */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory.(worktree/.git) */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The staging directory, restores string Blobs */
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");
    /**  */
    public static final File STAGE = join(GITLET_DIR, "stage");

    /* TODO: fill in the rest of this class. */


    /* TODO:checks
     *
     *
     *
     *
     *
     * */
}
