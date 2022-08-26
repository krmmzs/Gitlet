package gitlet;

import static gitlet.Utils.join;
import static gitlet.Utils.sha1;

import java.io.File;
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
    //TODO: more lazy loading.

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
     * The cache of saveFile.
     */
    private final File saveFile;

    /**
     * <pre>
     * The blobs of this Commit.
     *
     * filename, blob's id.
     * <pre>
     */
    private final HashMap<String, String> blobs;


    public Commit() {
        this.message = "initial commit";
        this.timestamp = new Date(0);
        this.parents = new LinkedList<>();
        this.blobs = new HashMap<>();
        this.id = sha1(message, timestamp.toString()); // init's id(sha1) is special.
        this.saveFile = generateSaveFile();
    }

    public Commit(String message, List<Commit> parents, Stage stage) {
        this.message = message;
        this.timestamp = new Date();
        // this.parents = parents;
        this.parents = new ArrayList<>(2);
        for (Commit t : parents) {
            this.parents.add(t.getId());
        }
        this.blobs = parents.get(0).getBlobs(); // using first parent blobs

        for (Map.Entry<String, String> entry : stage.getAdded().entrySet()) {
            String fileName = entry.getKey();
            String blobId = entry.getValue();
            blobs.put(fileName, blobId); // if same fileName, different blobId, will update
        }
        for (String fileName : stage.getRemoved()) {
            blobs.remove(fileName);
        }
        // this.blobs = blobs;
        this.id = generateId();
        this.saveFile = generateSaveFile();
    }

    public HashMap<String, String> getBlobs() {
        return this.blobs;
    }

    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getId() {
        return this.id;
    }

    public List<String> getParents() {
        return this.parents;
    }

    public File getSaveFile() {
        return saveFile;
    }

    public String getDateString() {
        DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return df.format(timestamp);
    }

    /**
     * Generate saveFile.
     *
     * @return SaveFile by id.
     */
    private File generateSaveFile() {
        return join(Repository.COMMIT_DIR, id); // now, without Tries firstly...
    }


    /**
     * Generate id.
     *
     * @return id by message, Timestamp, pearents, blobs.
     */
    private String generateId() {
        return sha1(message, timestamp.toString(), parents.toString(), blobs.toString());
    }

    public String getFirstParentId() {
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
}
