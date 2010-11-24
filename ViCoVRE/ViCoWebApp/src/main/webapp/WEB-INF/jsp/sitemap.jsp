<?xml version="1.0" encoding="UTF-8"?>
<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page" version="1.2"
        xmlns:c="http://java.sun.com/jsp/jstl/core">
    <jsp:directive.page contentType="text/xml"/>
    <urlset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9
                 http://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd
                 http://www.google.com/schemas/sitemap-video/1.1
                 http://www.google.com/schemas/sitemap-video/1.1/sitemap-video.xsd"
             xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
             xmlns:video="http://www.google.com/schemas/sitemap-video/1.1">
        <c:forEach items="${recordings}" var="recording">
            <url>
                <loc>${baseUrl}/${recording.folder}/${recording.id}/displayRecording.do</loc>
                <video:video>
                    <video:thumbnail_loc>${baseUrl}/video.jpg</video:thumbnail_loc>
                    <video:title>${recording.metadata.primaryValue}</video:title>
                    <video:description>${recording.metadata.primaryValue}</video:description>
                    <video:content_loc>${baseUrl}/${recording.folder}/${recording.id}/downloadRecording.do?format=video%2Fx-flv</video:content_loc>
                </video:video>
            </url>
            <c:forEach items="${annotations[recording.id]}" var="annotation">
                <c:set var="time" value="${annotation.timestamp - recording.startTime.time}"/>
                <url>
                    <loc>${baseUrl}/${recording.folder}/${recording.id}/displayRecording.do?startTime=${time}</loc>
                    <video:video>
                        <video:thumbnail_loc>${baseUrl}/video.jpg</video:thumbnail_loc>
                        <video:title>${recording.metadata.primaryValue}</video:title>
                        <video:description>${annotation.message}</video:description>
                        <video:content_loc>${baseUrl}/${recording.folder}/${recording.id}/downloadRecording.do?format=video%2Fx-flv&amp;offset=${time}</video:content_loc>
                        <c:forEach items="${annotation.tags}" var="tag">
                            <video:tag>${tag}</video:tag>
                        </c:forEach>
                    </video:video>
                </url>
            </c:forEach>
        </c:forEach>
    </urlset>
</jsp:root>