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
    private final File source;

    /**
     * The content of the Bolb.
     */
    private final byte[] content;

    /**
     * The file of this instance with the path generated from SHA1 id.
     */
    private final File saveFile;

    /**
     * Cache of file path.
     */
    private final String filePath = null;

    /**
     * Cache of file name.
     */
    private final String fileName = null;

    public Blob(File source) {
        this.source = source;
        this.content = generateContents();
        this.filePath = source.getPath();
        id = this.generateId();
        this.saveFile = generateSaveFile();
    }

	public String getId() {
		return id;
	}

	public byte[] getContent() {
		return content;
	}

	public File getSource() {
		return source;
	}

	public File getSaveFile() {
		return saveFile;
	}

	public String getFilePath() {
		return filePath;
	}

    public String getFileName() {
        if (fileName == null) {
            fileName = source.getName();
        }
		return fileName;
	}

    // Persistence
    private byte[] readFile() {
        return readContents(source);
    }

    private void writeFile() {
        writeObject(source, this);
    }


	private String generateId() {
        return sha1(filePath, content);
    }

    private byte[] generateContents() {
        readContents(source);
    }

    /**
     * @return save filename:id File.(like git)
     */
    private File generateSaveFile() {
        return join(Repository.OBJECTS_DIR, id);
    }
}
