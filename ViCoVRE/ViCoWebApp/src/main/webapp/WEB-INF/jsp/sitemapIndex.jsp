<?xml version="1.0" encoding="UTF-8"?>
<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page" version="1.2"
        xmlns:c="http://java.sun.com/jsp/jstl/core">
    <jsp:directive.page contentType="text/xml"/>
    <sitemapindex xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9
                 http://www.sitemaps.org/schemas/sitemap/0.9/siteindex.xsd"
             xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
        <c:forEach items="${folders}" var="folder">
            <sitemap>
                <loc><c:out escapeXml="true" value="${baseUrl}${folder}/sitemap.xml"/></loc>
            </sitemap>
        </c:forEach>
    </sitemapindex>
</jsp:root>