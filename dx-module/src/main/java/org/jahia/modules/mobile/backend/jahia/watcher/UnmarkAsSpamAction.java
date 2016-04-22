package org.jahia.modules.mobile.backend.jahia.watcher;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;

import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Created by loom on 21.05.15.
 */
public class UnmarkAsSpamAction extends Action {
    private static final String SPAM_DETECTED_MIXIN = "jmix:spamFilteringSpamDetected";
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(UnmarkAsSpamAction.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {

        String nodeIdentifier = httpServletRequest.getParameter("nodeIdentifier");

        JCRNodeWrapper targetNode = jcrSessionWrapper.getNodeByIdentifier(nodeIdentifier);
        if (!targetNode.hasPermission(Privilege.JCR_WRITE)) {
            return ActionResult.INTERNAL_ERROR;
        }

        if (!targetNode.isNodeType(SPAM_DETECTED_MIXIN)) {
            // is detected as spam -> add mixin
            logger.warn("Node is not marked as spam, can't unmark!");
            return ActionResult.BAD_REQUEST;
        } else {
            jcrSessionWrapper.checkout(targetNode);
            targetNode.removeMixin(SPAM_DETECTED_MIXIN);
            logger.info("Node " + targetNode.getPath() + " marked as spam removed !");
        }
        jcrSessionWrapper.save();

        return ActionResult.OK;
    }
}
