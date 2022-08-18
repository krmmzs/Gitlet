package gitlet;

import static gitlet.Utils.join;
import static gitlet.Utils.sha1;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date; // Represents Time.
import java.util.HashMap;
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
        this.parents = new ArrayList<>(); // need order(first parents... second parents) and better memory than LinkedList.
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
            String filename = entry.getKey();
            String bolbId = entry.getValue();
            blobs.put(filename, blobId);
        }
        for (String filename : stage.getRemoved()) {
            blobs.remove(filename);
        }
        // this.blobs = blobs;
        //HACK: There may be order issues
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
        return join(Repository.OBJECTS_DIR, id); // now, without Tries firstly...
    }


    /**
     * Generate id.
     *
     * @return id by message, Timestamp, pearents, blobs.
     */
    private String generateId() {
        return sha1(message, timestamp.toString(), parents.toString(), blobs.toString());
    }
}
