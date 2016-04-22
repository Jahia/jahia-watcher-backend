package org.jahia.modules.mobile.backend.jahia.watcher;

import org.apache.velocity.util.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLGenerator;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.services.usermanager.jcr.JCRUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                URLGenerator urlGenerator = renderContext.getURLGenerator();
                if (urlGenerator == null) {
                    urlGenerator = new URLGenerator(renderContext, resource);
                }
                String previewUrl = renderContext.getRequest().getContextPath() + urlGenerator.getBasePreview() + displayableNode.getPath() + ".html";
                jsonObject.put("preview-url", previewUrl);
            }

            boolean assignable = isAssignable(renderContext, candidates, assigneeUserKey);

            String taskStatus = null;
            if (task.hasProperty("state")) {
                taskStatus = task.getProperty("state").getString();
            }

            if ("active".equals(taskStatus) && assignable && !getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("Assign to me", "assignToMe"));
            }

            if ("active".equals(taskStatus) && getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("Refuse", "refuse"));
                possibleActions.add(new TaskAction("Start", "start"));
            }

            if ("started".equals(taskStatus) && getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("Refuse", "refuse"));
                possibleActions.add(new TaskAction("Suspend", "suspend"));
                if (task.hasProperty("possibleOutcomes")) {
                    List<String> possibleOutcomes = new ArrayList<String>();
                    JCRValueWrapper[] possibleOutcomesValues = task.getProperty("possibleOutcomes").getValues();
                    if (possibleOutcomesValues != null && possibleOutcomesValues.length > 0) {
                        for (JCRValueWrapper possibleOutcomeValue : possibleOutcomesValues) {
                            possibleActions.add(new TaskAction(StringUtils.capitalizeFirstLetter(possibleOutcomeValue.getString()), "finished", possibleOutcomeValue.getString()));
                        }
                    }
                } else {
                    possibleActions.add(new TaskAction("Finished", "finished"));
                }
            }

            if ("finished".equals(taskStatus)) {
                // nothing possible in this case
            }

            if ("suspended".equals(taskStatus) && getCurrentUser().getName().equals(assigneeUserKey)) {
                possibleActions.add(new TaskAction("Refuse", "refuse"));
                possibleActions.add(new TaskAction("Continue", "continue"));
            }

            if (possibleActions.size() > 0) {
                JSONArray jsonArray = new JSONArray();
                for (TaskAction taskAction : possibleActions) {
                    JSONObject jsonTaskAction = new JSONObject(taskAction);
                    jsonArray.put(jsonTaskAction);
                }
                jsonObject.put("possibleActions", jsonArray);
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
                List<String> userGroups = getUserMembership(getCurrentUser());
                for (String userGroup : userGroups) {
                    if (candidates.contains("g:" + userGroup) || candidates.contains(userGroup)) {
                        assignable = true;
                    }
                }
            }
        }
        return assignable;
    }

    protected List<String> getUserMembership(JahiaUser jahiaUser) {
        // because of an API change between DF 7.0 and 7.1 we use reflection API to make sure we call the proper
        // method.
        try {
            Method getUserMembershipMethod = jahiaGroupManagerService.getClass().getMethod("getUserMembership", JahiaUser.class);
            // we are in the DF 7.0 case
            return (List<String>) getUserMembershipMethod.invoke(jahiaGroupManagerService, jahiaUser);
        } catch (NoSuchMethodException e) {
            // we are in the DF 7.1 case
            try {
                Method getUserMembershipMethod = jahiaGroupManagerService.getClass().getMethod("getUserMembership", String.class, String.class);
                Method jahiaPrincipalMethod = jahiaUser.getClass().getMethod("getRealm");
                String realm = (String) jahiaPrincipalMethod.invoke(jahiaUser);
                return (List<String>) getUserMembershipMethod.invoke(jahiaGroupManagerService, jahiaUser.getUsername(), realm);
            } catch (NoSuchMethodException e1) {
                logger.error("Coudln't find any recognized getUserMembership method !", e1);
            } catch (InvocationTargetException e1) {
                logger.error("Error invoking getUserMembership method !", e1);
            } catch (IllegalAccessException e1) {
                logger.error("Error invoking getUserMembership method !", e1);
            }
        } catch (InvocationTargetException e) {
            logger.error("Error invoking getUserMembership method !", e);
        } catch (IllegalAccessException e) {
            logger.error("Error invoking getUserMembership method !", e);
        }
        return null;
    }


    private ActionResult doPost(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) {
        JSONObject jsonObject = new JSONObject();

        String action = httpServletRequest.getParameter("action");
        String outcome = httpServletRequest.getParameter("finalOutcome");

        JCRNodeWrapper task = resource.getNode();

        try {
            if (!task.isNodeType("jnt:task")) {
                return ActionResult.BAD_REQUEST;
            }

            jcrSessionWrapper.checkout(task);
            if ("assignToMe".equals(action)) {
                task.setProperty("state", "active");
                task.setProperty("assigneeUserKey", getCurrentUser().getName());
            } else if ("refuse".equals(action)) {
                task.setProperty("assigneeUserKey", (Value) null);
                task.setProperty("state", "active");
            } else if ("start".equals(action)) {
                task.setProperty("state", "started");
            } else if ("suspend".equals(action)) {
                task.setProperty("state", "suspended");
            } else if ("preview".equals(action)) {
                // we do nothing for the preview action
            } else if ("finished".equals(action)) {
                if (outcome != null) {
                    task.setProperty("finalOutcome", outcome);
                }
                task.setProperty("state", "finished");
            } else if ("continue".equals(action)) {
                task.setProperty("state", "started");
            } else {
                // this is probably a possible outcome, we will treat it as such
                if (outcome != null) {
                    task.setProperty("finalOutcome", outcome);
                }
                task.setProperty("state", "finished");
            }
            jcrSessionWrapper.save();
        } catch (RepositoryException e) {
            logger.error("Error updating task " + task.getPath(), e);
        }
        logger.info("Task " + task.getPath() + " updated successfully to action={} outcome={}", action, outcome);

        return new ActionResult(200, null, jsonObject);
    }
}
