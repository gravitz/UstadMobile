package com.ustadmobile.core.contentformats.xapi;

import java.util.Map;

public class Verb {

    private String id;

    private Map<String, String> display;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getDisplay() {
        return display;
    }

    public void setDisplay(Map<String, String> display) {
        this.display = display;
    }
}
