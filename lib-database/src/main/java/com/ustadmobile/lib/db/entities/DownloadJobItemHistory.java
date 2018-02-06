package com.ustadmobile.lib.db.entities;

import com.ustadmobile.lib.database.annotation.UmEntity;
import com.ustadmobile.lib.database.annotation.UmPrimaryKey;

/**
 * Created by mike on 2/2/18.
 */
@UmEntity
public class DownloadJobItemHistory {

    @UmPrimaryKey(autoIncrement = true)
    private int id;

    private String url;

    //Foreign key for the networknode this is connected to
    private int networkNode;

    private int mode;

    private long numBytes;

    private boolean successful;

    private long startTime;

    private long endTime;

    public DownloadJobItemHistory() {

    }

    public DownloadJobItemHistory(int networkNode, int mode, boolean successful, long startTime, long endTime){
        this.networkNode = networkNode;
        this.mode = mode;
        this.successful = successful;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public DownloadJobItemHistory(NetworkNode node, int mode, long startTime) {

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getNetworkNode() {
        return networkNode;
    }

    public void setNetworkNode(int networkNode) {
        this.networkNode = networkNode;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public long getNumBytes() {
        return numBytes;
    }

    public void setNumBytes(long numBytes) {
        this.numBytes = numBytes;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
