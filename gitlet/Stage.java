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
     * Staged for removal.
     *
     * <filename>
     */
    private Set<String> removed;

    public Stage() {
        added = new HashMap<>();
        removed = new HashSet<>();
    }
}
