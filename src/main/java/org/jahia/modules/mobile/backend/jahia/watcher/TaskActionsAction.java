package org.jahia.modules.mobile.backend.jahia.watcher;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Retrieves a list of possible actions given the tasks current status
 */
public class TaskActionsAction extends Action {
    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        if ("post".equals(httpServletRequest.getMethod().toLowerCase())) {
            return doPost(httpServletRequest, renderContext, resource, jcrSessionWrapper, map, urlResolver);
        } else {
            return doGetOrDefault(httpServletRequest, renderContext, resource, jcrSessionWrapper, map, urlResolver);
        }
        /*
                            <ul class="taskactionslist">
                                <c:set var="assignable" value="true" />
                                <c:if test="${not empty task.properties['candidates'] and task.properties['assigneeUserKey'].string ne user.name}">
                                    <c:set var="assignable" value="false" />
                                    <c:set var="candidates" value=""/>
                                    <c:forEach items="${task.properties['candidates']}" var="candidate">
                                        <c:set var="candidates" value=" ${candidate.string} ${candidates} "/>
                                    </c:forEach>
                                    <c:set var="userKey" value="u:${user.name}" />
                                    <c:if test="${fn:contains(candidates, userKey)}">
                                        <c:set var="assignable" value="true" />
                                    </c:if>
                                    <c:if test="${not assignable}">
                                        <c:set var="groups" value="${user:getUserMembership(user)}" />
                                        <c:forEach items="${groups}" var="x">
                                            <c:if test="${fn:contains(candidates, x.key)}">
                                                <c:set var="assignable" value="true" />
                                            </c:if>
                                        </c:forEach>
                                    </c:if>
                                </c:if>
                                <c:choose>
                                    <c:when test="${taskStatus == 'active' and task.properties['assigneeUserKey'].string ne user.name and assignable eq 'true'}">
                                        <li><a class="taskaction taskaction-assign" href="javascript:sendNewAssignee('${task.identifier}','${task.path}','${user.name}')" title="assign to me"><fmt:message key="label.actions.assigneToMe"/></a></li>
                                    </c:when>
                                    <c:when test="${taskStatus == 'active' and task.properties['assigneeUserKey'].string eq user.name}">
                                        <li><a class="taskaction taskaction-refuse" href="javascript:sendNewAssignee('${task.identifier}','${task.path}','')" title="Refuse"><fmt:message key="label.actions.refuse"/></a></li>
                                        <li><a class="taskaction taskaction-start" href="javascript:sendNewStatus('${task.identifier}','${task.path}','started')" title="start"><fmt:message key="label.actions.start"/></a></li>
                                    </c:when>
                                    <c:when test="${taskStatus == 'started' and task.properties['assigneeUserKey'].string eq user.name}">
                                        <li><a class="taskaction taskaction-refuse" href="javascript:sendNewAssignee('${task.identifier}','${task.path}','')" title="Refuse"><fmt:message key="label.actions.refuse"/></a></li>
                                        <li><a class="taskaction taskaction-suspend" href="javascript:sendNewStatus('${task.identifier}','${task.path}','suspended')" title="suspend"><fmt:message key="label.actions.suspend"/></a></li>
                                        <utility:setBundle basename="${task.properties['taskBundle'].string}" var="taskBundle"  />
                                        <c:if test="${not empty task.properties['targetNode'].node}">
                                        	<c:set var="currentTaskNode" value="${jcr:findDisplayableNode(task.properties['targetNode'].node, renderContext)}" />
                                            <li><a class="taskaction taskaction-preview" target="_blank" href="<c:url value="${url.basePreview}${currentTaskNode.path}.html"/>"><fmt:message key="label.preview"/></a></li>
                                        </c:if>
                                        <c:if test="${not empty task.properties['possibleOutcomes']}">
                                            <c:forEach items="${task.properties['possibleOutcomes']}" var="outcome" varStatus="status">
                                                <fmt:message bundle="${taskBundle}" var="outcomeLabel" key="${fn:replace(task.properties['taskName'].string,' ','.')}.${fn:replace(outcome.string,' ','.')}"/>
                                                <c:if test="${fn:startsWith(outcomeLabel, '???')}"><fmt:message bundle="${taskBundle}" var="outcomeLabel" key="${fn:replace(task.properties['taskName'].string,' ','.')}.${fn:replace(fn:toLowerCase(outcome.string),' ','.')}"/></c:if>
                                                <li><a class="taskaction taskaction-start" href="javascript:sendNewStatus('${task.identifier}','${task.path}','finished','${outcome.string}')" title="${outcome.string}">${outcomeLabel}</a></li>
                                            </c:forEach>
                                        </c:if>
                                        <c:if test="${empty task.properties['possibleOutcomes']}">
                                            <c:set var="taskId" value="${task.identifier}"/>
                                            <li><div class="taskaction-complete"><input class="completeTaskAction" taskPath="<c:url value='${url.base}${currentNode.path}'/>" type="checkbox" id="btnComplete-${taskId}" onchange="sendNewStatus('${taskId}','${task.path}','finished')"/>&nbsp;<label for="btnComplete-${taskId}"><fmt:message key="label.actions.completed"/></label></div></li>
                                        </c:if>
                                        <li>
                                            <jcr:node var="taskData" path="${task.path}/taskData"/>
                                            <c:if test="${not empty taskData}">
                                                <template:module path="${task.path}/taskData" view="taskData" />
                                            </c:if>
                                        </li>
                                    </c:when>
                                    <c:when test="${taskStatus == 'finished'}">
                                        <li><div class="taskaction-complete"><input name="Completed" type="checkbox" disabled="disabled" checked="checked" value="Completed" />&nbsp;<fmt:message key="label.actions.completed"/></div></li>
                                    </c:when>
                                    <c:when test="${taskStatus == 'suspended' and task.properties['assigneeUserKey'].string eq user.name}">
                                        <li><a class="taskaction taskaction-refuse" href="javascript:sendNewAssignee('${task.identifier}','${task.path}','')" title="Refuse"><fmt:message key="label.actions.refuse"/></a></li>
                                        <li><a class="taskaction taskaction-continue" href="javascript:sendNewStatus('${task.identifier}','${task.path}','started')" title="start"><fmt:message key="label.actions.resume"/></a></li>
                                    </c:when>
                                    <c:when test="${taskStatus == 'canceled'}">
                                    </c:when>
                                </c:choose>
                                <c:if test="${not empty task.properties['dueDate']}"><li><a class="taskaction taskaction-iCalendar" href="<c:url value='${url.base}${task.path}.ics'/>" title="iCalendar"><fmt:message key="label.actions.icalendar"/></a></li></c:if>
                            </ul>

         */
    }

    private ActionResult doGetOrDefault(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) {
        JSONObject jsonObject = new JSONObject();
        return new ActionResult(200, null, jsonObject);
    }

    private ActionResult doPost(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) {
        JSONObject jsonObject = new JSONObject();
        if ("assignToMe".equals(httpServletRequest.getParameter("action"))) {

        }
        return new ActionResult(200, null, jsonObject);
    }
}
