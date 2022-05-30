package com.orbital.scribex;

import java.io.Serializable;

public class ScribexUser implements Serializable {
    private final String uid;

    /**
     * Constructs a ScribexUser object. Currently, users are defined by their firebase ID only.
     * @param idToken   String idToken obtained from the FireBaseUser object by calling FireBaseUser::getUid
     */
    ScribexUser(String idToken) {
        this.uid = idToken;
    }

    public String getUid() {
        return uid;
    }
}
