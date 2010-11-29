<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <title>
            Recorder
        </title>

        <!-- Define some properties -->
        <script type="text/javascript" language="javascript">
        var Parameters = {
            url: "/rest/",
            playUrl: "/play.do",
            layouts: '${layoutsJSON}',
            customLayouts: '${customLayoutsJSON}',
            users: '${usersJSON}',
            groups: '${groupsJSON}'
        };
        </script>
        <script type="text/javascript" language="javascript" src="${pageContext.request.contextPath}/com.googlecode.vicovre.gwt.recorder.Application.nocache.js"></script>
    </head>
    <body>
        <iframe src="javascript:''" id="__gwt_historyFrame" tabIndex='-1' style="position:absolute;width:0;height:0;border:0"></iframe>
    </body>
</html>