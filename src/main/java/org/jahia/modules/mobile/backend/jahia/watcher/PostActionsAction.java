package org.jahia.modules.mobile.backend.jahia.watcher;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLGenerator;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.services.usermanager.jcr.JCRUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by loom on 19.05.15.
 */
public class PostActionsAction extends Action {
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(TaskActionsAction.class);

    private JahiaUserManagerService jahiaUserManagerService;

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        return doGetOrDefault(httpServletRequest, renderContext, resource, jcrSessionWrapper, map, urlResolver);
    }

    private ActionResult doGetOrDefault(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) {
        JSONObject jsonObject = new JSONObject();

        List<PostAction> possibleActions = new ArrayList<PostAction>();

        try {
            JCRNodeWrapper post = resource.getNode();

            if (!post.isNodeType("jnt:post")) {
                return ActionResult.BAD_REQUEST;
            }

            JCRNodeWrapper displayableNode = JCRContentUtils.findDisplayableNode(post, renderContext);
            URLGenerator urlGenerator = renderContext.getURLGenerator();
            if (urlGenerator == null) {
                urlGenerator = new URLGenerator(renderContext, resource);
            }
            String previewUrl = renderContext.getRequest().getContextPath() + urlGenerator.getBasePreview() + displayableNode.getPath() + ".html";
            jsonObject.put("view-url", previewUrl);

            if (post.hasPermission(Privilege.JCR_REMOVE_NODE)) {
                possibleActions.add(new PostAction("Delete", "delete"));
            }
            if (post.hasPermission(Privilege.JCR_WRITE)) {
                possibleActions.add(new PostAction("Mark as spam", "markAsSpam"));
            }
            if (post.getParent().hasPermission(Privilege.JCR_WRITE)) {
                possibleActions.add(new PostAction("Reply", "reply"));
            }
            if (post.hasProperty("jcr:createdBy")) {
                String author = post.getProperty("jcr:createdBy").getString();
                JahiaUser authorUser = jahiaUserManagerService.lookupUser(author);
                if (authorUser != null && authorUser instanceof JCRUser) {
                    JCRUser authorJCRUser = (JCRUser) authorUser;
                    if (authorJCRUser.getNode(jcrSessionWrapper).hasPermission(Privilege.JCR_MODIFY_PROPERTIES)) {
                        possibleActions.add(new PostAction("Block user", "blockUser"));
                    }
                }
            }

            if (possibleActions.size() > 0) {
                JSONArray jsonArray = new JSONArray();
                for (PostAction postAction : possibleActions) {
                    JSONObject jsonPostAction = new JSONObject(postAction);
                    jsonArray.put(jsonPostAction);
                }
                jsonObject.put("possibleActions", jsonArray);
            }
        } catch (RepositoryException e) {
            logger.error("Error accessing post " + resource.getNode(), e);
        } catch (JSONException e) {
            logger.error("Error generating JSON result", e);
        }


        return new ActionResult(200, null, jsonObject);
    }

}
