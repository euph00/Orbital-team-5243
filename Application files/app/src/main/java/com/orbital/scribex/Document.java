package com.orbital.scribex;

public class Document {
    private int id;
    private String name;
    private String dateTime;
    private String text;
    private String snippet;

    public Document(int id, String name, String dateTime, String text, String snippet) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.text = text;
        this.snippet = snippet;
    }

    public void setId(int id) {
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

    public int getId() {
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
