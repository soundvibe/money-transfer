package net.soundvibe.web;

public enum HttpHeader {

    JSON_CONTENT("content-type", "application/json");

    public final String name;
    public final String value;

    HttpHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
