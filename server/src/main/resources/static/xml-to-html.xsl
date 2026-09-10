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
	xmlns:res="http://www.w3.org/2005/sparql-results#" xmlns:url="xalan://java.net.URLEncoder"
	xmlns:ntriplesutil="xalan://com.bbn.parliament.jena.joseki.bridge.util.NTriplesUtil"
	exclude-result-prefixes="res xsl url">

	<xsl:output method="html" media-type="text/html" encoding="UTF-8" indent="yes"
		doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
		doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>

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
		<div>
			Count: <xsl:value-of select="count(res:results/res:result)"/>
			<br/>
			<table>
				<tr>
					<xsl:for-each select="res:head/res:variable">
						<th><xsl:value-of select="@name"/></th>
					</xsl:for-each>
				</tr>
				<xsl:for-each select="res:results/res:result">
					<tr>
						<xsl:apply-templates select="."/>
					</tr>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

	<xsl:template match="res:result">
		<xsl:variable name="current" select="."/>
		<xsl:for-each select="/res:sparql/res:head[1]/res:variable">
			<xsl:variable name="name" select="@name"/>
			<td>
				<xsl:choose>
					<xsl:when test="$current/res:binding[@name=$name]">
						<!-- apply template for the correct value type (bnode, uri, literal) -->
						<xsl:apply-templates select="$current/res:binding[@name=$name]"/>
					</xsl:when>
					<xsl:otherwise>
						<!-- no binding available for this variable in this solution -->
					</xsl:otherwise>
				</xsl:choose>
			</td>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="res:bnode">
		<xsl:text>_:</xsl:text>
		<xsl:value-of select="text()"/>
	</xsl:template>

	<xsl:template match="res:uri">
		<xsl:variable name="uri" select="text()"/>
		<xsl:variable name="euri">
			<xsl:choose>
				<xsl:when test="function-available('url:encode')">
					<xsl:value-of select="url:encode(ntriplesutil:escapeString($uri))"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="url-encode">
						<xsl:with-param name="str" select="$uri"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<a href="explorer.jsp?value=%3C{$euri}%3E"><xsl:value-of select="$uri"/></a>
	</xsl:template>

	<xsl:template match="res:literal">
		<xsl:variable name="literal" select="text()"/>
		<xsl:variable name="eliteral">
			<xsl:choose>
				<xsl:when test="function-available('url:encode')">
					<xsl:value-of select="url:encode(ntriplesutil:escapeString($literal))"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="url-encode">
						<xsl:with-param name="str" select="$literal"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<a href="explorer.jsp?value=%22{$eliteral}%22">
			<xsl:text>"</xsl:text>
			<xsl:value-of select="$literal"/>
			<xsl:text>"</xsl:text>
		</a>
		<xsl:choose>
			<xsl:when test="@datatype">
				<!-- datatyped literal value -->
				^^&lt;<xsl:value-of select="@datatype"/>&gt;
			</xsl:when>
			<xsl:when test="@xml:lang">
				<!-- lang-string -->
				@<xsl:value-of select="@xml:lang"/>
			</xsl:when>
		</xsl:choose>
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



	<!--
		ISO-8859-1 based URL-encoding demo. Written by Mike J. Brown, mike@skew.org.
		Updated 2002-05-20.

		No license; use freely, but credit me if reproducing in print.

		Also see http://skew.org/xml/misc/URI-i18n/ for a discussion of non-ASCII
		characters in URIs.
	-->

	<!-- The string to URL-encode. Note: By "iso-string" we mean a Unicode string
		where all the characters happen to fall in the ASCII and ISO-8859-1 ranges
		(32-126 and 160-255) -->

	<!-- Characters we'll support. We could add control chars 0-31 and 127-159,
		but we won't. -->
	<xsl:variable name="ascii">
		!"#$%&amp;'()*+,-./0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~
	</xsl:variable>
	<xsl:variable name="latin1">
		&#160;&#161;&#162;&#163;&#164;&#165;&#166;&#167;&#168;&#169;&#170;&#171;&#172;&#173;&#174;&#175;&#176;&#177;&#178;&#179;&#180;&#181;&#182;&#183;&#184;&#185;&#186;&#187;&#188;&#189;&#190;&#191;&#192;&#193;&#194;&#195;&#196;&#197;&#198;&#199;&#200;&#201;&#202;&#203;&#204;&#205;&#206;&#207;&#208;&#209;&#210;&#211;&#212;&#213;&#214;&#215;&#216;&#217;&#218;&#219;&#220;&#221;&#222;&#223;&#224;&#225;&#226;&#227;&#228;&#229;&#230;&#231;&#232;&#233;&#234;&#235;&#236;&#237;&#238;&#239;&#240;&#241;&#242;&#243;&#244;&#245;&#246;&#247;&#248;&#249;&#250;&#251;&#252;&#253;&#254;&#255;
	</xsl:variable>

	<!-- Characters that usually don't need to be escaped -->
	<xsl:variable name="safe">
		!'()*-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~
	</xsl:variable>

	<xsl:variable name="hex">
		0123456789ABCDEF
	</xsl:variable>

	<xsl:template name="url-encode">
		<xsl:param name="str"/>
		<xsl:if test="$str">
			<xsl:variable name="first-char" select="substring($str,1,1)"/>
			<xsl:choose>
				<xsl:when test="contains($safe,$first-char)">
					<xsl:value-of select="$first-char"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:variable name="codepoint">
						<xsl:choose>
							<xsl:when test="contains($ascii,$first-char)">
								<xsl:value-of select="string-length(substring-before($ascii,$first-char)) + 32"/>
							</xsl:when>
							<xsl:when test="contains($latin1,$first-char)">
								<xsl:value-of select="string-length(substring-before($latin1,$first-char)) + 160"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:message terminate="no">
									Warning: string contains a character that is out of range!
									Substituting "?".
								</xsl:message>
								<xsl:text>63</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:variable name="hex-digit1"
						select="substring($hex,floor($codepoint div 16) + 1,1)"/>
					<xsl:variable name="hex-digit2"
						select="substring($hex,$codepoint mod 16 + 1,1)"/>
					<xsl:value-of select="concat('%',$hex-digit1,$hex-digit2)"/>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:if test="string-length($str) &gt; 1">
				<xsl:call-template name="url-encode">
					<xsl:with-param name="str" select="substring($str,2)"/>
				</xsl:call-template>
			</xsl:if>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
