package org.jahia.modules.mobile.backend.jahia.watcher;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Created by loom on 17.04.15.
 */
public class MarkAsSpamAction extends Action {

    private static final String SPAM_DETECTED_MIXIN = "jmix:spamFilteringSpamDetected";
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(MarkAsSpamAction.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {

        String nodeIdentifier = httpServletRequest.getParameter("nodeIdentifier");

        logger.info("Marking node "+nodeIdentifier+" as spam");

        // @todo add permission check

        JCRNodeWrapper targetNode = jcrSessionWrapper.getNodeByIdentifier(nodeIdentifier);
        if (!targetNode.isNodeType(SPAM_DETECTED_MIXIN)) {
            // is detected as spam -> add mixin
            jcrSessionWrapper.checkout(targetNode);
            targetNode.addMixin(SPAM_DETECTED_MIXIN);
            logger.info("Node " + targetNode.getPath() + " marked as spam");
        } else {
            jcrSessionWrapper.checkout(targetNode);
            targetNode.removeMixin(SPAM_DETECTED_MIXIN);
            logger.info("Node " + targetNode.getPath() + " marked as spam removed !");
        }
        jcrSessionWrapper.save();

        return null;
    }

}
