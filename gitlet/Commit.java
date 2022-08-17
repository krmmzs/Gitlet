package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date; // Represents Time.
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static gitlet.Utils.*;

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
        this.id = generateId();
        this.saveFile = generateSaveFile();
    }

    public Commit(String message, List<String> parents, Map<String, String> blobs) {
        this.message = message;
        this.timestamp = new Date();
        this.parents = parents;
        this.blobs = blobs;
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

    // Persistence
    //TODO: design Persistence for Commit.

    /**
     * @param commit Commit Object which will be Serialized.
     */
    private void writeToFile() {
        if (saveFile == null) {
        this.saveFile = join(COMMIT_DIR, this.getId()); // now, without Tries firstly...
        }
        writeObject(saveFile, this);
    }

    private File generateSaveFile() {
        return join(Repository.OBJECTS_DIR, id);
    }


    public String generateId() {
        return sha1(message, timestamp.toString(), parents.toString(), blobs.toString());
    }
}
