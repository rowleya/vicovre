<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html style="width: 100%; height: 100%; padding: 0; margin: 0;">
    <head>
        <title>
            <c:if test="${not empty recording}">
                ${recording.metadata.primaryValue}
            </c:if>
            <c:if test="${empty recording}">
                Recording Not Found
            </c:if>
        </title>
        <!-- Define some properties -->
        <script type="text/javascript" language="javascript">
        var Parameters = {
            url: "/rest/",
            playUrl: "/play.do",
            recording: "${recording.id}",
            folder: "${recording.folder}",
            users: '${usersJSON}',
            groups: '${groupsJSON}',
            role: "${role}",
            canEdit: "${recording.editable}",
            canPlay: "${recording.playable}",
            acl: '${aclJSON}',
            readAcl: '${readAclJSON}',
            startTime: '${startTime}',
            agc: '${agc}'
        };
        </script>
        <script type="text/javascript" language="javascript" src="${pageContext.request.contextPath}/com.googlecode.vicovre.gwt.display.Application.nocache.js"></script>
    </head>
    <body>
        <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>
    </body>
</html>