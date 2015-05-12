package org.jahia.modules.mobile.backend.jahia.watcher;

/**
 * Created by loom on 12.05.15.
 */
public class TaskAction {

    private String name;
    private String finalOutcome;

    public TaskAction(String name, String finalOutcome) {
        this.name = name;
        this.finalOutcome = finalOutcome;
    }

    public TaskAction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getFinalOutcome() {
        return finalOutcome;
    }
}
