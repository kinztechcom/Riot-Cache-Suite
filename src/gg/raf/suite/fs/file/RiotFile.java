package gg.raf.suite.fs.file;

import java.io.*;

/**
 * Created by Allen Kinzalow on 9/24/2015.
 *
 * RiotFile serves to represent the raw data of a single file in the
 *  League of Legends cache. This single file can be identified by the
 *  path list index which correlates to a given directory.
 */
public class RiotFile {

    /**
     * The hash of the fs's path.
     */
    private int hash;

    /**
     * The offset of the data archive fs.
     */
    private int dataOffset;

    /**
     * The length of the data in the data archive fs.
     */
    private int dataSize;

    /**
     * The index of the path in the list decoded in the archive header.
     *  Value between 0 and pathlist - 1.
     */
    private int pathListIndex;

    /**
     * The fs data.
     */
    private byte[] fileData;

    public RiotFile(int hash, int dataOffset, int dataSize, int pathListIndex) {
        this.hash = hash;
        this.dataOffset = dataOffset;
        this.dataSize = dataSize;
        this.pathListIndex = pathListIndex;
        this.fileData = new byte[this.dataSize];
    }

    /**
     * Get the hash.
     * @return
     */
    public int getHash() {
        return hash;
    }

    /**
     * Return the data offset in the raf.dat fs
     * @return
     */
    public int getDataOffset() {
        return dataOffset;
    }

    /**
     * Return the length of the fs entry in the raf.dat fs.
     * @return
     */
    public int getDataSize() {
        return dataSize;
    }

    /**
     * Return the index of this fs entry in the path list decoded in the archive header.
     * @return
     */
    public int getPathListIndex() {
        return pathListIndex;
    }

    /**
     * Return the fs data for this entry.
     * @return
     */
    public byte[] getFileData() {
        return fileData;
    }

    /**
     * Set the fs data for this entry.
     * @param fileData
     */
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    /**
     * An "abstract" method for subclasses to utilize for
     *  further decoding of the file byte data.
     */
    public void decode() { }

    /**
     * Save the file data to a given file.
     * @param file
     */
    public void saveFileData(File file) {
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file));
            outputStream.write(this.getFileData());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}