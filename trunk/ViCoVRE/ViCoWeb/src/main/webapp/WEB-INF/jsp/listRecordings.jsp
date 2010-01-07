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
                ${folder.name}
            </c:if>
        </title>
    </head>
    <body>
        <c:if test="${empty folder}">
            <h1>Folder not found!</h1>
        </c:if>
        <c:if test="${not empty folder}">
            <h1>${folder.name}</h1>
            <table border="0">
                <c:if test="${not isTopLevel}">
                    <tr><td><img src="${pageContext.request.contextPath}/images/folder.gif"/></td><td><a href="../listRecordings.do"><b>..</b></a></td></tr>
                </c:if>
                <c:forEach items="${folder.folders}" var="subfolder">
                    <tr>
                        <td><img src="${pageContext.request.contextPath}/images/folder.gif"/></td>
                        <td><a href="${subfolder.name}/listRecordings.do"><b>${subfolder.name}</b></a></td></tr>
                </c:forEach>
                <c:forEach items="${folder.recordings}" var="recording">
                    <tr>
                        <td><img src="${pageContext.request.contextPath}/images/recording.gif"/></td>
                        <td><a href="${recording.id}/displayRecording.do">${recording.metadata.name}</a></td>
                        <td><fmt:formatDate value="${recording.startTime}" pattern="yyyy-MM-dd HH:mm"/><br/></td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
    </body>
</html>