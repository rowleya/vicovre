<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
    <head>
        <title>
            <c:if test="${not empty recording}">
                ${recording.metadata.name}
            </c:if>
            <c:if test="${empty recording}">
                Recording Not Found
            </c:if>
        </title>
        <script type="text/javascript" src="/js/swfobject.js"></script>
    </head>
    <body>
        <c:if test="${empty recording}">
            <h1>Recording Not Found</h1>
        </c:if>
        <c:if test="${not empty recording}">
            <!-- div that displays the player -->
            <div id="player">&nbsp;</div>

            <!-- JS to setup and control the player -->
            <script type="text/javascript">

                document.getElementById("player").innerHTML =
                    "Flash is required to replay this video."
                    + "  Please click "
                    + "<a href=\"http://get.adobe.com/flashplayer/\">here</a>"
                    + " to get Flash.";
                var mplayer = new SWFObject("${pageContext.request.contextPath}/CrewPlayer.swf", "mplayer",
                        "${width}", "${height}", "8");
                mplayer.addParam("AllowScriptAccess", "always");
                mplayer.addParam("allowfullscreen", "false");
                mplayer.addVariable("uri", "${pageContext.request.contextPath}/play.do?folder=${folder}%26recordingId=${recording.id}%26startTime=0");
                mplayer.write("player");
            </script>
        </c:if>
    </body>
</html>