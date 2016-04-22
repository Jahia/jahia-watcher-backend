package org.jahia.modules.mobile.backend.jahia.watcher;

/**
 * Created by loom on 19.05.15.
 */
public class PostAction {

    private String displayName;
    private String name;

    public PostAction(String displayName, String name) {
        this.displayName = displayName;
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }
}
