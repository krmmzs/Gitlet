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
     * The content of the Bolb.
     */
    private final byte[] content;

    /**
     * Cache of file name.
     */
    private final String fileName;

    /**
     * construct Blob with file name and where.
     * @param fileName
     * @param CWD
     */
    public Blob(String fileName, File CWD) {
        this.fileName = fileName;
        File file = join(CWD, fileName);
        if (file.exists()) {
            this.content = readContents(file);
            this.id = generateId();
        } else {
            this.content = null;
            this.id = sha1(fileName);
        }
    }

	public String getId() {
		return id;
	}

	public byte[] getContent() {
		return content;
	}

	private String generateId() {
        return sha1(fileName, content);
    }

	public String getFileName() {
		return fileName;
	}

    public boolean exists() {
        return this.content != null;
    }

    /**
     * @return save filename:id File.(like git)
     */
    private File generateSaveFile() {
        return join(Repository.BLOBS_DIR, id);
    }

}
