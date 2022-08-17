package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

/**
 * Blob
 */
public class Blob implements Serializable {
    /**
     * The reference to the Blob.
     */

    private final String id;
    /**
     * The file name of the Bolb.
     */
    private final String filename;

    /**
     * The content of the Bolb.
     */
    private final byte[] content;

    public Blob(String filename, File CWD) {
        this.filename = filename;
        File file = join(CWD, filename);
        if (file.exists()) {
            this.content = readContents(file);
            this.id = sha1(filename, content);
        }
        else {
            this.content = null;
            this.id = sha1(filename);
        }
    }

    public String getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }

}
