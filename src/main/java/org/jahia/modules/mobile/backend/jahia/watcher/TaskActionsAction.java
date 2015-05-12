package org.jahia.modules.mobile.backend.jahia.watcher;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Retrieves a list of possible actions given the tasks current status
 */
public class TaskActionsAction extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(TaskActionsAction.class);

    private JahiaGroupManagerService jahiaGroupManagerService;

    public void setJahiaGroupManagerService(JahiaGroupManagerService jahiaGroupManagerService) {
        this.jahiaGroupManagerService = jahiaGroupManagerService;
    }

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        if ("post".equals(httpServletRequest.getMethod().toLowerCase())) {
            return doPost(httpServletRequest, renderContext, resource, jcrSessionWrapper, map, urlResolver);
        } else {
            return doGetOrDefault(httpServletRequest, renderContext, resource, jcrSessionWrapper, map, urlResolver);
        }
    }

    private ActionResult doGetOrDefault(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) {
        JSONObject jsonObject = new JSONObject();

        List<TaskAction> possibleActions = new ArrayList<TaskAction>();

        try {
            JCRNodeWrapper task = resource.getNode();

            if (!task.isNodeType("jnt:task")) {
                return ActionResult.BAD_REQUEST;
            }

            Set<String> candidates = new HashSet<String>();
            if (task.hasProperty("candidates")) {
                JCRValueWrapper[] candidatesValues = task.getProperty("candidates").getValues();
                if (candidatesValues != null && candidatesValues.length > 0) {
                    for (JCRValueWrapper candidatesValue : candidatesValues) {
                        candidates.add(candidatesValue.getString());
                    }
                }
            }
            String assigneeUserKey = null;
            if (task.hasProperty("assigneeUserKey")) {
                assigneeUserKey = task.getProperty("assigneeUserKey").getString();
            }

            if (task.hasProperty("targetNode")) {
                JCRNodeWrapper targetNode = (JCRNodeWrapper) task.getProperty("targetNode").getNode();
                JCRNodeWrapper displayableNode = JCRContentUtils.findDisplayableNode(targetNode, renderContext);
                String previewUrl = renderContext.getRequest().getContextPath() + renderContext.getURLGenerator().getBasePreview() + displayableNode.getPath() + ".html";
                jsonObject.put("preview-url", previewUrl);
            }

            boolean assignable = isAssignable(renderContext, candidates, assigneeUserKey);

            String taskStatus = null;
            if (task.hasProperty("state")) {
                taskStatus = task.getProperty("state").getString();
            }

            if ("active".equals(taskStatus) && assignable && !getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("assignToMe"));
            }

            if ("active".equals(taskStatus) && getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("refuse"));
                possibleActions.add(new TaskAction("start"));
            }

            if ("started".equals(taskStatus) && getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("refuse"));
                possibleActions.add(new TaskAction("suspend"));
                if (task.hasProperty("possibleOutcomes")) {
                    List<String> possibleOutcomes = new ArrayList<String>();
                    JCRValueWrapper[] possibleOutcomesValues = task.getProperty("possibleOutcomes").getValues();
                    if (possibleOutcomesValues != null && possibleOutcomesValues.length > 0) {
                        for (JCRValueWrapper possibleOutcomeValue : possibleOutcomesValues) {
                            possibleActions.add(new TaskAction("finished", possibleOutcomeValue.getString()));
                        }
                    }
                } else {
                    possibleActions.add(new TaskAction("finished"));
                }
            }

            if ("finished".equals(taskStatus)) {
                // nothing possible in this case
            }

            if ("suspended".equals(taskStatus) && getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("refuse"));
                possibleActions.add(new TaskAction("continue"));
            }

            if (possibleActions.size() > 0) {
                jsonObject.put("possibleActions", possibleActions);
            }
        } catch (RepositoryException e) {
            logger.error("Error accessing task " + resource.getNode(), e);
        } catch (JSONException e) {
            logger.error("Error generating JSON result", e);
        }


        return new ActionResult(200, null, jsonObject);
    }

    private boolean isAssignable(RenderContext renderContext, Set<String> candidates, String assigneeUserKey) {
        boolean assignable = true;
        if (candidates.size() > 0 && !renderContext.getUser().getName().equals(assigneeUserKey)) {
            assignable = false;
            if (candidates.contains("u:" + getCurrentUser().getName())) {
                assignable = true;
            }
            if (!assignable) {
                List<String> userGroups = jahiaGroupManagerService.getUserMembership(getCurrentUser());
                for (String userGroup : userGroups) {
                    if (candidates.contains(userGroup)) {
                        assignable = true;
                    }
                }
            }
        }
        return assignable;
    }

    private ActionResult doPost(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) {
        JSONObject jsonObject = new JSONObject();

        String action = httpServletRequest.getParameter("action");
        String outcome = httpServletRequest.getParameter("outcome");

        JCRNodeWrapper task = resource.getNode();

        try {
            if (!task.isNodeType("jnt:task")) {
                return ActionResult.BAD_REQUEST;
            }

            jcrSessionWrapper.checkout(task);
            if ("assignToMe".equals(action)) {
                task.setProperty("assigneeUserKey", getCurrentUser().getUserKey());
            } else if ("refuse".equals(action)) {
                task.setProperty("assigneeUserKey", (Value) null);
            } else if ("start".equals(action)) {
                task.setProperty("state", "started");
            } else if ("suspend".equals(action)) {
                task.setProperty("state", "suspended");
            } else if ("preview".equals(action)) {
                // we do nothing for the preview action
            } else if ("finished".equals(action)) {
                task.setProperty("state", "finished");
            } else if ("continue".equals(action)) {
                task.setProperty("state", "started");
            } else {
                // this is probably a possible outcome, we will treat it as such
                task.setProperty("state", "finished");
                if (outcome != null) {
                    task.setProperty("finalOutcome", outcome);
                }
            }
            jcrSessionWrapper.save();
        } catch (RepositoryException e) {
            logger.error("Error updating task " + task.getPath(), e);
        }


        return new ActionResult(200, null, jsonObject);
    }
}
