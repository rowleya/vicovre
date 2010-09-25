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
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/swfobject.js"></script>
    </head>
    <body style="width: 100%; height: 100%; padding: 0; margin: 0;">
        <c:if test="${empty recording}">
            <h1>Recording Not Found</h1>
        </c:if>
        <c:if test="${not empty recording}">
            <!-- div that displays the player -->
            <div id="player" style="width: 100%; height: 100%">&nbsp;</div>

            <!-- JS to setup and control the player -->
            <script type="text/javascript">

                document.getElementById("player").innerHTML =
                    "Flash is required to replay this video."
                    + "  Please click "
                    + "<a href=\"http://get.adobe.com/flashplayer/\">here</a>"
                    + " to get Flash.";
                var mplayer = new SWFObject("${pageContext.request.contextPath}/Player.swf", "mplayer",
                        "100%", "100%", "8");
                mplayer.addParam("AllowScriptAccess", "always");
                mplayer.addParam("allowfullscreen", "false");
                mplayer.addVariable("uri", "${pageContext.request.contextPath}/play.do?folder=${folder}%26recordingId=${recording.id}%26startTime=0");
                mplayer.write("player");
            </script>
        </c:if>
    </body>
</html>