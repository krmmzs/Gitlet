package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.Utils.*;

/**
 * Blob
 */
public class Blob implements Serializable {

    /**
     * The reference to the Blob.
     */
    private String id;

    /**
     * The content of the Bolb.
     */
    private byte[] content;

    /**
     * Cache of file name.
     */
    private String fileName;

    /**
     * File create path.
     */
    private File cwd;

    /**
     * construct Blob with file name and where.
     * @param fileName
     * @param cwd
     */
    public Blob(String fileName, File cwd) {
        this.fileName = fileName;
        this.cwd = cwd;
    }

    public String getId() {
        if (id == null) {
            this.id = generateId();
        }
        return this.id;
    }

    public byte[] getContent() {
        if (content == null) {
            this.content = generateContent();
        }
        return content;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean exists() {
        return this.content != null;
    }

    public String getContentAsString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    private byte[] generateContent() {
        File file = join(cwd, fileName);
        if (file.exists()) {
            return readContents(file);
        } else {
            return null;
        }
    }

    private String generateId() {
        File file = join(cwd, fileName);
        if (file.exists()) {
            return sha1(fileName, this.getContent());
        } else {
            return sha1(fileName);
        }
    }
}
