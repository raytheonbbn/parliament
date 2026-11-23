<?xml version="1.0"?>

<!--

XSLT script to format SPARQL Query Results XML Format into xhtml

Copyright (c) 2004, 2005 World Wide Web Consortium, (Massachusetts Institute of
Technology, European Research Consortium for Informatics and Mathematics, Keio
University). All Rights Reserved. This work is distributed under the W3C Software
License [1] in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

[1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231

Version 1 : Dave Beckett (DAWG)
Version 2 : Jeen Broekstra (DAWG)
Customization for SPARQler: Andy Seaborne

-->

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml"
	xmlns:res="http://www.w3.org/2005/sparql-results#"
	exclude-result-prefixes="res xsl">

	<xsl:output method="html" media-type="text/html"
		doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" indent="yes"
		encoding="UTF-8"/>

	<xsl:template name="header">
		<div>
			<h2>Header</h2>
			<xsl:for-each select="res:head/res:link">
				<p>Link to <xsl:value-of select="@href"/></p>
			</xsl:for-each>
		</div>
	</xsl:template>

	<xsl:template name="boolean-result">
		<div>
			<p>ASK => <xsl:value-of select="res:boolean"/></p>
		</div>
	</xsl:template>

	<xsl:template name="vb-result">
		<div>Count: <xsl:value-of select="count(res:results/res:result)"/></div>
	</xsl:template>

	<xsl:template match="res:sparql">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
			<head>
				<link rel="stylesheet" type="text/css" href="stylesheets/queryResults.css"/>
				<title>SPARQLer Query Results</title>
			</head>
			<body>
				<h1>SPARQLer Query Results</h1>
				<div class="op-list">
					<h3><a href="index.jsp">Home</a></h3>
					<h2>Operations</h2>
					<ul>
						<li><a href="query.jsp">Query</a></li>
						<li><a href="explorerIndex.jsp">Explore</a></li>
						<li><a href="update.jsp">SPARQL/Update</a></li>
						<li><a href="insert.jsp">Insert Data</a></li>
						<li><a href="export.jsp">Export</a></li>
						<li><a href="indexes.jsp">Indexes</a></li>
						<li><a href="admin.jsp">Admin</a></li>
					</ul>
				</div>
				<br/>

				<xsl:if test="res:head/res:link">
					<xsl:call-template name="header"/>
				</xsl:if>

				<xsl:choose>
					<xsl:when test="res:boolean">
						<xsl:call-template name="boolean-result"/>
					</xsl:when>
					<xsl:when test="res:results">
						<xsl:call-template name="vb-result"/>
					</xsl:when>
				</xsl:choose>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
