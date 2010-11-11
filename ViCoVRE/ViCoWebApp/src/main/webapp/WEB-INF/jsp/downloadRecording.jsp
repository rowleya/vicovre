<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
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
            recording: "${recording.id}",
            duration: ${recording.duration},
            streams: '${streamsJSON}',
            folder: "${folder}",
            layouts: '${layoutsJSON}',
            customLayouts: '${customLayoutsJSON}',
            canPlay: ${canPlay},
            role: "${role}"
        };
        </script>
        <script type="text/javascript" language="javascript" src="${pageContext.request.contextPath}/com.googlecode.vicovre.gwt.download.Application.nocache.js"></script>
    </head>
    <body>
        <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>
    </body>
</html>