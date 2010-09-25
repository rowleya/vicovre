<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<% pageContext.setAttribute("newLineChar", "\n"); %>
<html>
    <head>
        <title>
            <c:if test="${empty folder}">
                Folder not found!
            </c:if>
            <c:if test="${not empty folder}">
                ${folder}
            </c:if>
        </title>
    </head>
    <body>
        <c:if test="${empty folder}">
            <h1>Folder not found!</h1>
        </c:if>
        <c:if test="${not empty folder}">
            <h1>${folder}</h1>
            <table border="0">
                <c:if test="${not isTopLevel}">
                    <tr><td><img src="${pageContext.request.contextPath}/images/folder.gif"/></td><td><a href="../listRecordings.do"><b>..</b></a></td></tr>
                </c:if>
                <c:forEach items="${subfolders}" var="subfolder">
                    <tr>
                        <td><img src="${pageContext.request.contextPath}/images/folder.gif"/></td>
                        <td><a href="${subfolder}/listRecordings.do"><b>${subfolder}</b></a></td></tr>
                </c:forEach>
                <c:forEach items="${recordings}" var="recording">
                    <tr>
                        <td><img src="${pageContext.request.contextPath}/images/recording.gif"/></td>
                        <c:if test="${recording.playable}">
                            <td><a href="${recording.id}/displayRecording.do">${recording.metadata.primaryValue}</a></td>
                        </c:if>
                        <c:if test="${not recording.playable}">
                            <td>${recording.metadata.primaryValue} is not accessible to you.</td>
                        </c:if>
                        <td><fmt:formatDate value="${recording.startTime}" pattern="yyyy-MM-dd HH:mm"/><br/></td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
    </body>
</html>