package gitlet;

import static gitlet.Utils.sha1;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Date; // Represents Time.
import java.util.HashMap;
// import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  Having our metadata consist only of
 *  a timestamp and log message. A commit, therefore,
 *  will consist of a log message, timestamp, a mapping 
 *  of file names to blob references, a parent reference,
 *  and (for merges) a second parent reference.
 *
 *  @author krmmzs
 */
public class Commit implements Serializable {

    /**
     * The message of this Commit.
     */
    private String message;

    /**
     * The timestamp of this Commit.
     */
    private Date timestamp;

    /**
     * The id(SHA1) of this Commit.
     */
    private String id;

    /**
     * The parents of this Commit(id->String).
     */
    private List<String> parents;

    /**
     * Cache for parents
     */
    private transient List<Commit> parentsExt;

    /**
     * Cache for stage.
     */
    private Stage stage;

    /**
     * <pre>
     * The blobs of this Commit.
     *
     * filename, blob's id.
     * <pre>
     */
    private Map<String, String> blobs;


    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
        this.parents = new LinkedList<>();
        this.blobs = new HashMap<>();
        this.id = sha1(message, timestamp.toString()); // init's id(sha1) is special.
    }

    public Commit(String message, List<Commit> parentsExt, Stage stage) {
        this.message = message;
        this.timestamp = new Date();
        this.parentsExt = parentsExt;
        this.stage = stage;
    }

    public Map<String, String> getBlobs() {
        if (this.blobs == null) {
            generateBlobs();
        }
        return this.blobs;
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getId() {
        if (this.id == null) {
            this.id = generateId();
        }
        return this.id;
    }

    public List<String> getParents() {
        if (this.parents == null) {
            generateParents();
        }
        return this.parents;
    }

    private void generateParents() {
        this.parents = new ArrayList<>(2);
        for (Commit t : this.parentsExt) {
            this.parents.add(t.getId());
        }
    }

    public String getDateString() {
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return df.format(timestamp);
    }

    public String getFirstParentId() {
        if (parents == null) {
            generateParents();
        }
        if (parents.isEmpty()) {
            // original return "null"
            return "";
        }
        return parents.get(0);
    }

    public String getCommitAsString() {
        StringBuffer sb = new StringBuffer();
        sb.append("===\n");
        sb.append("commit " + this.id + "\n");
        if (parents.size() == 2) {
            sb.append("Merge: " + parents.get(0).substring(0, 7)
                + " " + parents.get(1).substring(0, 7) + "\n");
        }
        sb.append("Date: " + this.getDateString() + "\n");
        sb.append(this.message + "\n\n");
        return sb.toString();
    }

    /**
     * Lazy load generate id.
     *
     * @return id by message, Timestamp, pearents, blobs.
     */
    private String generateId() {
        if (parents == null) {
            generateParents();
        }
        if (blobs == null) {
            generateBlobs();
        }
        return sha1(message, timestamp.toString(), parents.toString(), blobs.toString());
    }

    private void generateBlobs() {
        this.blobs = parentsExt.get(0).getBlobs(); // using first parent blobs
        for (Map.Entry<String, String> entry : stage.getAdded().entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue();
            blobs.put(fileName, blobId); // if same fileName, different blobId, will update
        }
        for (String fileName : stage.getRemoved()) {
            blobs.remove(fileName);
        }
    } 
}
