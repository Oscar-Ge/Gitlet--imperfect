package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *
 *  does at a high level.
 *
 *  @author Oscar Ge
 */
public class Repository {
    /**
     *
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    /**
     * design philosophy:
     * only save the blobs and commits once in the objects directory,
     * while the other files use the ID to refer to them.
     *
     * TO NOTICE:
     *  writeObject --> readObject
     *  writeContents --> readContentsAsString
     *   .gitlet
     *      |--objects
     *      |    |--commits (Name: 40-digit commit ID; contents: Commit.class)
     *      |     |--blobs (individual files) (Name: 40-digit blob ID; contents: Blob.class)
     *      |--refs
     *      |    |--heads // just save 40 digits of commit ID stored
     *      |         |--master (file) (name: branch name; contents: 40-digit commit ID)
     *      |         |--....(other branches, like 61abc...) (files)
     *      |--branch (file) // branch at now (Name: branch; contents the String of current branch)
     *      |--HEAD (file) // 40 digits of commit ID stored (Name: HEAD; contents: 40-digit commit ID)
     *      |--stage (in these files: name: fileName; contents: 40-digits blobID)
     *      |   |--addStage
     *      |   |--removeStage
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
        public static final File HEAD = join(GITLET_DIR, "HEAD"); // have been madeNewFile in the Commit() <init> method
        public static final File branch = join(GITLET_DIR, "branch");
        public static final File stage = join(GITLET_DIR, "stage");
            public static final File addStage = join(stage, "addStage");
            public static final File removeStage = join(stage, "removeStage");
        public static final File objects = join(GITLET_DIR, "objects");
            public static final File commits = join(objects, "commits");
        public static final File refs = join(GITLET_DIR, "refs");
            public static final File heads = join(refs, "heads");
                public static final File master = join(heads, "master");

    public static void init()
    {
        GITLET_DIR.mkdir();
        stage.mkdir();
        addStage.mkdir();
        removeStage.mkdir();
        objects.mkdir();
        commits.mkdir();
        refs.mkdir();
        heads.mkdir();
        String moRen_branch = "master";
        writeContents(branch, moRen_branch);
        Commit initialCommit = new Commit();
        Utils.writeContents(HEAD, initialCommit.readID());
        initialCommit.save(readContentsAsString(branch));
    }

    public static void addFile(String filename) {
        if (checkGitlet()) return;
        Commit m = ReadHEADCommit();
        File file = join(CWD, filename);
        List<String> removeFiles = plainFilenamesIn(removeStage);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        if (removeFiles != null && removeFiles.contains(filename)) {
            File f = join(removeStage, filename);
            f.delete();
            return;
        }
        if (m.getPathToblobID(filename) != null) {
            Blob b = readObject(join(objects, m.getPathToblobID(filename)), Blob.class);
            if (b != null) {
                if (Arrays.equals(b.getContents(), readContents(file)))
                    return;
            }
        }
        Blob blob = new Blob(filename);
        File staage = join(addStage, filename);
        if (!staage.getParentFile().exists()){
            staage.getParentFile().mkdirs();
        }
        Utils.writeObject(staage, blob.readID()); // write its ID to the addStage
        blob.save(); // save it to the objects
    }

    public static void newCommit(String message)
    {
        if (checkGitlet()) return;
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        Commit m = new Commit();
        m.setMessage(message);
        boolean isAddStage = addBlobsInStage(m);
        boolean isRemoveStage = removeBlobsInStage(m);
        if (!isAddStage && !isRemoveStage) {
            System.out.println("No changes added to the commit.");
            return;
        }
        m.save(readContentsAsString(branch)); // save to the refs and branches
        Utils.writeObject(HEAD, m.readID()); // save it to the HEAD
    }


    private static boolean addBlobsInStage(Commit commit)
    {
        List<String> files = plainFilenamesIn(addStage);
        if (files == null) {
            return false;
        }
        if (files.isEmpty()) {
            return false;
        }
        for (String file : files) {
            File blobFile = join(addStage, file);
            String blobID = Utils.readContentsAsString(blobFile);
            commit.addPathToblobID(file, blobID);
        }
        CleanUp(addStage);
        return true;
    }

    private static boolean removeBlobsInStage(Commit commit)
    {
        List<String> files = plainFilenamesIn(removeStage);
        if (files == null) {
            return false;
        }
        if (files.isEmpty()) {
            return false;
        }
        for (String file : files) {
            commit.removePathToblobID(file);
        }
        CleanUp(removeStage);
        return true;
    }

    public static void removeFile(String filename)
    {
        if (checkGitlet()) return;
        File file = join(CWD, filename);
        File addStageFile = join(addStage, filename);
        Commit head = ReadHEADCommit();
        if (head.getPathToblobID(filename) == null && !addStageFile.exists())
        {
            System.out.println("No reason to remove the file.");
            return;
        }

        List<String> addStageFiles = plainFilenamesIn(addStage);
        if (addStageFiles != null && addStageFiles.contains(filename))
        {
            File blobFile = join(addStage, filename);
            blobFile.delete();
            return;
        }
        if (file.exists() && head.getPathToblobID(filename) != null){
            Utils.restrictedDelete(file);
        }
        File removeStageFile = join(removeStage, filename);
        if (!removeStageFile.getParentFile().exists()){
            removeStageFile.getParentFile().mkdirs();
        }
        Utils.writeObject(removeStageFile, filename);
    }

    public static void log() {
        if (checkGitlet()) return;
        Commit head = ReadHEADCommit();
        while (head != null) {
            System.out.println(head.printCommit());
            String parentID = head.getParentID();
            if (parentID == null) {
                break;
            }
            head = Utils.readObject(join(commits, parentID), Commit.class);
        }
    }

    public static void globalLog() {
        if (checkGitlet()) return;
        List<String> commitFiles = plainFilenamesIn(commits);
        for (String commitFile : commitFiles) {
            Commit commit = Utils.readObject(join(commits, commitFile), Commit.class);
            System.out.println(commit.printCommit());
        }
    }

    public static void find(String message) {
        if (checkGitlet()) return;
        boolean isFind = findCommit(message);
        if (!isFind)
        {
            System.out.println("Found no commit with that message.");
        }
    }

    private static boolean findCommit (String message) {
        boolean isFind = false;
        List<String> commitFiles = plainFilenamesIn(commits);
        for (String commitFile : commitFiles) {
            Commit commit = Utils.readObject(join(commits, commitFile), Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.readID());
                isFind = true;
            }
        }
        return isFind;
    }

    public static void status()
    {
        if (checkGitlet()) return;
        System.out.println("=== Branches ===");
        List<String> branches = plainFilenamesIn(Repository.heads);
        for (String branchFile : branches) {
            if (branchFile.equals(readContentsAsString(branch))) {
                System.out.println("*" + branchFile);
            } else {
                System.out.println(branchFile);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> files = plainFilenamesIn(addStage);
        if (files != null) {
            for (String file : files) {
                System.out.println(file);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        files = plainFilenamesIn(removeStage);
        if (files != null)
        {
            for (String file : files) {
                System.out.println(file);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void FirstCheckout(String filename) {
        if (checkGitlet()) return;
        Commit m = ReadHEADCommit();
        SecondCheckout(m.readID(), filename);
    }

    public static void SecondCheckout(String commitID, String filename) {
        if (checkGitlet()) return;
        List<String> commitList = Utils.plainFilenamesIn(commits);
        for (String aCommitID : commitList) {
            if (commitID.substring(0, 6).equals(aCommitID.substring(0, 6))) {
                commitID = aCommitID;
                break;
            }
        }
        if (!join(commits, commitID).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit m = Utils.readObject(join(commits, commitID), Commit.class);
        if (m.getPathToblobID(filename) == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File blobFile = join(objects, m.getPathToblobID(filename));
        Blob blob = Utils.readObject(blobFile, Blob.class);
        recoverContentsToCWD(blob);
    }

    public static void ThirdCheckout (String branchName) {
        if (checkGitlet()) return;
        List<String> branches = plainFilenamesIn(Repository.heads);
        if (!branches.contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (Objects.equals(branchName, readContentsAsString(Repository.branch))) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Commit ini = ReadHEADCommit();
        String mID = Utils.readObject(join(heads, branchName), String.class);
        Commit m = Utils.readObject(join(commits, mID), Commit.class);
        if (m == null) {
            System.out.println("No such branch exists.");
            return;
        }
        Utils.writeContents(branch, branchName);
        checkOutCommit(ini, m);
    }

    private static void checkOutCommit(Commit ini, Commit m) {
        Utils.writeObject(HEAD, m.readID()); // save it to the HEAD
        List<String> CWDFile = plainFilenamesIn(CWD);
        Map<String, String> allFileMap = new HashMap<>();
        saveCommitFiles(ini, allFileMap);
        saveCommitFiles(m, allFileMap);
        Set<String> allFileSet = new HashSet<>();
        for (String files : allFileMap.keySet()) {
            allFileSet.add(files);
        }
        for (String file: allFileSet) { //TODO: debug this for loop to find the source of errors
            if (ini.getPathToblobID(file) != null && m.getPathToblobID(file) != null)
            {
                if (!ini.getPathToblobID(file).equals(m.getPathToblobID(file))) {
                    String FileID = m.getPathToblobID(file);
                    Blob b = Utils.readObject(join(objects, FileID), Blob.class);
                    recoverContentsToCWD(b);
                    // System.out.println("m1");
                }
            } else if (ini.getPathToblobID(file) != null && m.getPathToblobID(file) == null) {
                File Ffile = join(CWD, file);
                Ffile.delete();
                // System.out.println("m2");
            } else if (ini.getPathToblobID(file) == null && m.getPathToblobID(file) != null) {
                Blob b = Utils.readObject(join(objects, m.getPathToblobID(file)), Blob.class);
                File f = join(CWD, file);
                if (f.exists()) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }
                recoverContentsToCWD(b);
                // System.out.println("m3");
            }
        }
        CleanUp(addStage);
        CleanUp(removeStage);
    }

    public static void branch(String branch) {
        if (checkGitlet()) return;
        if (Utils.plainFilenamesIn(heads).contains(branch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        Utils.writeObject(join(heads, branch), ReadHEADCommit().readID());
    }

    public static void rmBranch(String branch) {
        if (checkGitlet()) return;
        if (!Utils.plainFilenamesIn(heads).contains(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (Objects.equals(branch, readContentsAsString(Repository.branch))) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        join(heads, branch).delete();
    }

    public static void reset(String commitID) {
        if (checkGitlet()) return;
        if (!Utils.plainFilenamesIn(commits).contains(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit ini = ReadHEADCommit();
        Commit m = Utils.readObject(join(commits, commitID), Commit.class);
        Utils.writeObject(HEAD, commitID);
        Utils.writeObject(join(heads, readContentsAsString(Repository.branch)), commitID);
        checkOutCommit(ini, m);
    }

    public static void merge(String branchName) {
        if (checkGitlet()) return;
        if (!plainFilenamesIn(heads).contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (plainFilenamesIn(addStage) != null || plainFilenamesIn(removeStage) != null) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (Objects.equals(branchName, readContentsAsString(Repository.branch))) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String CurrentBranch = readContentsAsString(branch);
        Commit CurrentHead = ReadHEADCommit();
        String newID = readObject(join(heads, branchName), String.class);
        Commit newBranchHead = readObject(join(commits, newID), Commit.class);
        Map<String, Integer> CurBranchDepth = branchDepth(CurrentHead);
        Map<String, Integer> newBranchDepth = branchDepth(newBranchHead);
        String minKey = findIniCommit(CurrentHead).readID();
        int minValue = Math.min(CurBranchDepth.size(), newBranchDepth.size());
        boolean IsFind = false;
        for (String commitID: CurBranchDepth.keySet()) {
            if (newBranchDepth.containsKey(commitID)) {
                if (!IsFind) {
                    IsFind = true;
                    minKey = commitID;
                    minValue = CurBranchDepth.get(commitID);
                } else if (CurBranchDepth.get(commitID) < minValue) {
                    minKey = commitID;
                    minValue = CurBranchDepth.get(commitID);
                }
            }
        }
        if (CurrentHead.isMerged()) {
            CurrentHead.setParentToMergedOnes();
            Map<String, Integer> CurBranchDepth2 = branchDepth(CurrentHead);
            for (String commitID: CurBranchDepth2.keySet()) {
                if (newBranchDepth.containsKey(commitID)) {
                    if (!IsFind) {
                        IsFind = true;
                        minKey = commitID;
                        minValue = CurBranchDepth2.get(commitID);
                    } else if (CurBranchDepth2.get(commitID) < minValue) {
                        minKey = commitID;
                        minValue = CurBranchDepth2.get(commitID);
                    }
                }
            }
            CurrentHead.setParentToOriginalOnes();
        } else if (newBranchHead.isMerged()) {
            newBranchHead.setParentToMergedOnes();
            Map<String, Integer> newBranchDepth2 = branchDepth(newBranchHead);
            for (String commitID: newBranchDepth2.keySet()) {
                if (CurBranchDepth.containsKey(commitID)) {
                    if (!IsFind) {
                        IsFind = true;
                        minKey = commitID;
                        minValue = newBranchDepth2.get(commitID);
                    } else if (newBranchDepth2.get(commitID) < minValue) {
                        minKey = commitID;
                        minValue = newBranchDepth2.get(commitID);
                    }
                }
            }
            newBranchHead.setParentToOriginalOnes();
        }
        Commit splitPoint = readObject(join(commits, minKey), Commit.class);
        if (splitPoint.readID().equals(CurrentHead.readID())) {
            checkOutCommit(CurrentHead, newBranchHead);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (splitPoint.readID().equals(newBranchHead.readID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        Commit mergeCommit = new Commit();
        String message = "Merged " + branchName + " into " + readContentsAsString(branch) + ".";
        mergeCommit.setMessage(message);
        Map<String, String> allFileMap = new HashMap<>(), splitMap = new HashMap<>(),
                iniMap = new HashMap<>(), newMap = new HashMap<>();
        saveCommitFiles(CurrentHead, allFileMap);
        saveCommitFiles(newBranchHead, allFileMap);
        saveCommitFiles(splitPoint, allFileMap);
        saveCommitFiles(splitPoint, splitMap);
        saveCommitFiles(CurrentHead, iniMap);
        saveCommitFiles(newBranchHead, newMap);
        for (String fileName: allFileMap.keySet()) {
            if (CurrentHead.getPathToblobID(fileName) == null && newBranchHead.getPathToblobID(fileName) != null) {
                File f = join(CWD, fileName);
                if (f.exists()) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
        for (String fileName: allFileMap.keySet()) {
            if (CurrentHead.getPathToblobID(fileName) != null
                    && newBranchHead.getPathToblobID(fileName) != null
                    && splitPoint.getPathToblobID(fileName) != null) {
                String CurBlobID = CurrentHead.getPathToblobID(fileName),
                        newBlobID = newBranchHead.getPathToblobID(fileName),
                        spBlobID = splitPoint.getPathToblobID(fileName);
                if (newBlobID.equals(spBlobID)) {
                    saveCommitBlobs(mergeCommit, CurBlobID);
                }else if (CurBlobID.equals(spBlobID)) {
                    saveCommitBlobs(mergeCommit, newBlobID);
                }
                 else  {
                    if (newBlobID.equals(CurBlobID)) {
                        saveCommitBlobs(mergeCommit, CurBlobID);
                    } else {
                        mergeConflict(CurBlobID, newBlobID, mergeCommit, fileName);
                    }
                }
            } else if (CurrentHead.getPathToblobID(fileName) != null
                    && newBranchHead.getPathToblobID(fileName) == null
                    && splitPoint.getPathToblobID(fileName) == null) {
                Blob blob = readObject(join(objects, CurrentHead.getPathToblobID(fileName)), Blob.class);
                saveCommitBlobs(mergeCommit, blob);
            } else if (CurrentHead.getPathToblobID(fileName) == null
                    && newBranchHead.getPathToblobID(fileName) != null
                    && splitPoint.getPathToblobID(fileName) == null) {
                Blob blob = readObject(join(objects, newBranchHead.getPathToblobID(fileName)), Blob.class);
                saveCommitBlobs(mergeCommit, blob);
                File f = join(CWD, fileName);
                writeContents(f, blob.getContents());
            } else if (CurrentHead.getPathToblobID(fileName) == null
                    && newBranchHead.getPathToblobID(fileName) != null
                    && splitPoint.getPathToblobID(fileName) != null) {
                byte[] newCon = readObject(join(objects, newBranchHead.getPathToblobID(fileName)), Blob.class).getContents(),
                        spCon = readObject(join(objects, splitPoint.getPathToblobID(fileName)), Blob.class).getContents();
                if (Arrays.equals(newCon, spCon)) {
                    mergeCommit.rmPathToblobID(fileName);
                } else {
                    System.out.println("Encountered a merge conflict.");
                    String newFile = new String(newCon);
                    String curFile = "";
                    mergeConflictFile(curFile, newFile, mergeCommit, fileName);
                }
            } else if (CurrentHead.getPathToblobID(fileName) != null
                    && newBranchHead.getPathToblobID(fileName) == null
                    && splitPoint.getPathToblobID(fileName) != null) {
                byte[] CurCon = readObject(join(objects, CurrentHead.getPathToblobID(fileName)), Blob.class).getContents(),
                        spCon = readObject(join(objects, splitPoint.getPathToblobID(fileName)), Blob.class).getContents();
                if (Arrays.equals(CurCon, spCon)) {
                    mergeCommit.rmPathToblobID(fileName);
                    join(CWD, fileName).delete();
                } else {
                    System.out.println("Encountered a merge conflict.");
                    String curFile = new String(CurCon);
                    String newFile = "";
                    mergeConflictFile(curFile, newFile, mergeCommit, fileName);
                }
            }
        }
        mergeCommit.merged(CurrentHead.readID(), newBranchHead.readID());
        mergeCommit.save(readContentsAsString(branch));
        Utils.writeObject(join(Repository.refs, "heads", branchName), mergeCommit.readID());
        writeObject(HEAD, mergeCommit.readID());
    }

    private static void mergeConflict (String CurBlobID, String newBlobID, Commit mergeCommit, String fileName) {
        System.out.println("Encountered a merge conflict.");
        String curFile = new String(readObject(join(objects, CurBlobID), Blob.class).getContents());
        String newFile = new String(readObject(join(objects, newBlobID), Blob.class).getContents());
        mergeConflictFile(curFile, newFile, mergeCommit, fileName);
    }

    private static void mergeConflictFile (String curFile, String newFile, Commit mergeCommit, String fileName) {
        String Con = "<<<<<<< HEAD\n" + curFile + "=======\n" + newFile + ">>>>>>>\n";
        File f = join(CWD, fileName);
        writeContents(f, Con);
        Blob newBlob = new Blob(fileName);
        newBlob.save();
        saveCommitBlobs(mergeCommit, newBlob.readID());
    }

    private static Map<String, Integer> branchDepth (Commit m) {
        Map<String, Integer> branchDepths = new HashMap<>();
        int depth = 0;
        while (!m.getDate().equals(new Date(0))) {
            branchDepths.put(m.readID(), depth);
            depth++;
            m = m.getParent();
        }
        branchDepths.put(m.readID(), depth);
        return branchDepths;
    }

    private static void saveCommitFiles (Commit m, Map<String, String> n) {
        Map<String, String> blobMap = m.getPathMap();
        for (String file: blobMap.keySet()) {
            if (!n.containsKey(file)) {
                n.put(file, m.getPathToblobID(file));
            }
        }
    }

    private static void saveCommitBlobs (Commit m, Blob blob) {
        m.addPathToblobID(blob.getFileName(), blob.readID());
        recoverContentsToCWD(blob);
    }

    private static void saveCommitBlobs (Commit m, String blobID) {
        saveCommitBlobs(m, readObject(join(objects, blobID), Blob.class));
    }

    private static boolean checkGitlet() {
        if (!GITLET_DIR.exists())
        {
            System.out.println("Not in an initialized Gitlet directory.");
            return true;
        }
        return false;
    }
    private static void CleanUp(File file)
    {
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            if (files == null)
            {
                return;
            }
            for (File f : files)
            {
                CleanUp(f);
            }
        }
        file.delete();
    }

    private static Commit ReadHEADCommit() {
        String HEADCommitID = ReadIDFromString(Utils.readContentsAsString(HEAD));
        Commit m = Utils.readObject(join(commits, HEADCommitID), Commit.class);
        if (m == null) {
            throw new GitletException("Found no commit with that id.");
        }
        return m;
    }

    private static String ReadIDFromString(String originalID) {
        return originalID.substring(originalID.length() - 40);
    }

    private static Commit readCommits (String CommitID) {
        return readObject(join(commits, CommitID), Commit.class);
    }

    private static Blob readBlob (String BlobID) {
        return readObject(join(objects, BlobID), Blob.class);
    }

    private static void recoverContentsToCWD (Blob blob) {
        File f = join(CWD, blob.getFileName());
        if (f.exists()) {
            f.delete();
        }
        writeContents(f, blob.getContents());
    }

    private static Commit findIniCommit (Commit m) {
        while (!m.getDate().equals(new Date(0))) {
            m = m.getParent();
        }
        return m;
    }


}
