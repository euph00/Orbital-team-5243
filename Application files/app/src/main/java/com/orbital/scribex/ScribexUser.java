package com.orbital.scribex;

import java.io.Serializable;

public class ScribexUser implements Serializable {
    private final String uid;

    ScribexUser(String idToken) {
        this.uid = idToken;
    }

    public String getUid() {
        return uid;
    }
}
