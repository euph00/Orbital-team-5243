package com.orbital.scribex;


import android.net.Uri;

import java.util.Date;

public class Photo {

    private Uri localUri;
    private Uri remoteUri;
    private Date dateTaken;
    private String id;

    /**
     * Creates an abstraction of a Photo taken by the user.
     * @param localUri  uri on local storage. Invoke FileProvider::getUriForFile and pass here.
     * @param remoteUri uri on Firebase Storage. This field will be set by UploadImageActivity::upload when firebase storage receives image.
     * @param dateTaken initialised by calling Date::new when Photo object is created
     * @param id    id assigned by firebase firestore. Assigned automatically by UploadImageActivity::UpdatePhotoDatabase when firestore is updated.
     */
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
