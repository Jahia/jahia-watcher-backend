package org.jahia.modules.mobile.backend.jahia.watcher;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;

import javax.jcr.security.Privilege;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Created by loom on 21.05.15.
 */
public class UnblockUserAction extends Action {

    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(UnblockUserAction.class);

    private JahiaUserManagerService jahiaUserManagerService;

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        String userName = req.getParameter("userName");

        JahiaUser jahiaUser = jahiaUserManagerService.lookupUser(userName);
        if (jahiaUser == null) {
            return ActionResult.INTERNAL_ERROR;
        }

        if (jahiaUser.isRoot() || !jahiaUser.isAccountLocked()) {
            return ActionResult.INTERNAL_ERROR;
        }

        JCRNodeWrapper userNode = session.getNode(jahiaUser.getLocalPath());
        if (!userNode.hasPermission(Privilege.JCR_MODIFY_PROPERTIES)) {
            return ActionResult.BAD_REQUEST;
        }

        if (!jahiaUser.isAccountLocked()) {
            return ActionResult.BAD_REQUEST;
        }

        logger.info("Unblocking user " + userName);
        jahiaUser.removeProperty("j:accountLocked");

        logger.info("Account has been unblocked.");

        return ActionResult.OK;
    }
}
