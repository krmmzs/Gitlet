package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stage
 */
public class Stage implements Serializable {

    /**
     * Staged for addition.
     *
     * <file name, blob's id>
     */
    private Map<String, String> added;

    /**
     * <pre>
     * Staged for removal.
     *
     * <file name>
     * <pre>
     */
    private Set<String> removed;

    public Stage() {
        added = new HashMap<>();
        removed = new HashSet<>();
    }

	public Map<String, String> getAdded() {
		return this.added;
	}

	public Set<String> getRemoved() {
		return this.removed;
	}

    public void add(String fileName, String blobId) {
        added.put(fileName, blobId);
        removed.remove(fileName);
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }
}
