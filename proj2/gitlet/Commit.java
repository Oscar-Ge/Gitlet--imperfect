package gitlet;


import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *
 *  does at a high level.
 *
 *  @author Oscar Ge
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Date date;
    private ArrayDeque<String> parents;
    private Map<String, String> pathToblobID;
    private String id;
    private File commitFile;
    private boolean isMerged;
    private String mergingCommit, mergedCommit;
    public Commit() {
        isMerged = false;
        if (!Repository.HEAD.exists()) {
            this.message = "initial commit";
            this.date = new Date(0);
            this.parents = new ArrayDeque<>();
            this.pathToblobID = new HashMap<>();
            this.id = this.generateID();
            this.commitFile = join(Repository.commits, this.id);
        }
        else {
            String parentID = Utils.readContentsAsString(Repository.HEAD).substring(Utils.readContentsAsString(Repository.HEAD).length() - 40);
            Commit parent = Utils.readObject(join(Repository.commits, parentID), Commit.class);
            this.message = parent.message;
            this.date = new Date();
            this.parents = parent.parents;
            this.parents.add(parent.readID());
            this.pathToblobID = parent.pathToblobID;
            this.id = this.generateID();
            this.commitFile = join(Repository.commits, this.id);
        }
    }

    private String generateID() {
        return Utils.sha1(dateToTimeStamp(this.date), message, parents.toString(), pathToblobID.toString());
    }

    public void merged (String iniID, String mID) {
        isMerged = true;
        mergingCommit = iniID;
        mergedCommit = mID;
    }

    public void save(String branch) {
        Utils.writeObject(commitFile, this);
        Utils.writeObject(join(Repository.refs, "heads", branch), this.id);
    }

    private static String dateToTimeStamp (Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
    public boolean isMerged () {
        return isMerged;
    }

    public String readID ()
    {
        return id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return this.date;
    }

    public void setCurrentDate() {
        this.date = new Date();
    }

    public void addParent(String parentID) {
        this.parents.add(parentID);
    }

    public Commit getParent () {
        return readObject(join(Repository.commits, getParentID()), Commit.class);
    }

    public String getParentID () {
        if (this.parents.isEmpty()) {
            return null;
        }
        return this.parents.getLast();
    }

    public void setParentToMergedOnes () {
        parents = new ArrayDeque<>();
        parents.addFirst(mergedCommit);
    }

    public void setParentToOriginalOnes () {
        Commit parent = readObject(join(Repository.commits, mergingCommit), Commit.class);
        parents = parent.parents;
        parents.addLast(parent.readID());
    }

    public void addPathToblobID(String path, String blobID) {
        if (this.pathToblobID.put(path, blobID) != null) {
            this.pathToblobID.remove(path);
            this.pathToblobID.put(path, blobID);
        } else {
            this.pathToblobID.put(path, blobID);
        }
    }

    public void rmPathToblobID (String path) {
        this.pathToblobID.remove(path);
    }

    public Map<String, String> getPathMap () {
        return this.pathToblobID;
    }

    public void removePathToblobID(String path) {
        this.pathToblobID.remove(path);
    }

    public String getPathToblobID(String path) {
        if (this.pathToblobID.get(path) == null) {
            return null;
        }
        return this.pathToblobID.get(path).substring(this.pathToblobID.get(path).length() - 40);
    }

    public Map<String, String> getPathToblobID() {
        return this.pathToblobID;
    }

    public String printCommit() {
        if (!isMerged) {
            String s = "===\n" + "commit " + this.id + "\nDate: " + dateToTimeStamp(this.date) + "\n" + this.message + "\n";
            return s;
        } else {
            String s = "===\n" + "commit " + this.id + "\nMerge: " + mergingCommit.substring(0, 7) + " " + mergedCommit.substring(0, 7) + "\nDate: " + dateToTimeStamp(this.date) + "\n" + this.message + "\n";
            return s;
        }

    }


}
