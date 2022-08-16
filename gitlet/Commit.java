package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date; // Represents Time.
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author krmmzs
 */
public class Commit implements Serializable{

    /**
     * The message of this Commit.
     */
    private final String message;

    /**
     * The timestamp of this Commit.
     */
    private final Date timestamp;

    /**
     * The id(SHA1) of this Commit.
     */
    private final String id;

    /**
     * The parents of this Commit.
     */
    private final List<String> parents;

    /**
     * <pre>
     * The blobs of this Commit.
     *
     * filename, blob's id.
     * <pre>
     */
    private final Map<String, String> blobs;

    /**
     * The file of this instance from SHA1 id;
     */
    private final File file;


    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
        this.id = sha1(message, timestamp.toString());
        this.parents = new ArrayList<>(); // need order(first parents... second parents) and better memory than LinkedList.
        this.blobs = new HashMap<>();
    }

    // public Commit(String message, List<String> parents, )
}
