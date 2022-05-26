package com.orbital.scribex;


import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Date;

public class Photo {

    private Uri localUri;
    private Uri remoteUri;
    private Date dateTaken;
    private String id;

    Photo(Uri localUri, Uri remoteUri, Date dateTaken, String id) {
        this.localUri = localUri;
        this.remoteUri = remoteUri;
        this.dateTaken = dateTaken;
        this.id = id;
    }

    public Uri getLocalUri() {
        return localUri;
    }

    public void setLocalUri(Uri localUri) {
        this.localUri = localUri;
    }

    public Uri getRemoteUri() {
        return remoteUri;
    }

    public void setRemoteUri(Uri remoteUri) {
        this.remoteUri = remoteUri;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
