package com.orbital.scribex;

import java.io.Serializable;

public class Document implements Serializable {

    private static final int SNIPPET_LENGTH = 30;

    private String id;
    private String name;
    private String dateTime;
    private String text;
    private String snippet;

    /**
     * Constructs a java object representing a scanned document.
     * @param id    Unique ID representing the document's identity.
     * @param name  Name of the document, as chosen by the user.
     * @param dateTime  Date and time that the document was created.
     * @param text  Scanned content of the document as pulled from FireBase.
     */
    public Document(String id, String name, String dateTime, String text) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.text = text;
        this.snippet = text.substring(0, Math.min(text.length(), SNIPPET_LENGTH));
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getText() {
        return text;
    }

    public String getSnippet() {
        return snippet;
    }
}
