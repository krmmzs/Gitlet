package gitlet;

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
     * <filename, blob's id>
     */
    private Map<String, String> added;

    /**
     * <pre>
     * Staged for removal.
     *
     * <filename>
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

	public void setAdded(Map<String, String> added) {
		this.added = added;
	}

	public Set<String> getRemoved() {
		return this.removed;
	}

	public void setRemoved(Set<String> removed) {
		this.removed = removed;
	}
}
