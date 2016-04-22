package org.jahia.modules.mobile.backend.jahia.watcher;

/**
 * Created by loom on 12.05.15.
 */
public class TaskAction {

    private String displayName;
    private String name;
    private String finalOutcome;

    public TaskAction(String displayName, String name, String finalOutcome) {
        this.displayName = displayName;
        this.name = name;
        this.finalOutcome = finalOutcome;
    }

    public TaskAction(String name) {
        this.displayName = name;
        this.name = name;
    }

    public TaskAction(String displayName, String name) {
        this.displayName = displayName;
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }

    public String getFinalOutcome() {
        return finalOutcome;
    }
}
