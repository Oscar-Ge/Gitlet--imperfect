package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import static gitlet.Utils.*;

public class Blob implements Serializable {
    private String id;
    private byte[] bytes;
    private String fileName;
    private String filePath;
    private File blobSaveFilePath;
    private File blobFile;

    public Blob(String file) {
        this.fileName = file;
        this.blobFile = join(Repository.CWD, file);
        this.filePath = blobFile.getPath();
        this.bytes = Utils.readContents(blobFile);
        this.id = this.generateID();
        this.blobSaveFilePath = join(Repository.GITLET_DIR, "objects", this.id);
    }

    private String generateID() {
        return Utils.sha1(this.filePath, Arrays.toString(bytes), this.fileName.toString());
    }

    public String readID() {
        return this.id;
    }

    public byte[] getContents() {
        return this.bytes;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public File getBlobSaveFilePath() {
        return this.blobSaveFilePath;
    }

    public void save() {
        Utils.writeObject(blobSaveFilePath, this);
    }

    public void save(File file) {
        Utils.writeObject(file, this);
    }
}
