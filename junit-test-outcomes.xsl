<?xml version="1.0" encoding="UTF-8"?>

<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!--
	Invoke from within a Subversion working copy via the command:
		svn log <double-hyphen>xml https://projects.semwebcentral.org/svn/parliament | xsltproc svn-user-names.xslt - | sort -f -u
	-->

	<xsl:output method="text"/>
	<xsl:param name="fileDir">.</xsl:param>

	<xsl:template match="/">
		<xsl:apply-templates select="//testsuite"/>
	</xsl:template>

	<xsl:template match="testsuite">
		<xsl:call-template name="lastPathComponent">
			<xsl:with-param name="path" select="$fileDir"/>
		</xsl:call-template>
			<xsl:text> - </xsl:text>
			<xsl:value-of select="@name"/><xsl:text>:&#x000a;</xsl:text>
		<xsl:text>   Tests run: </xsl:text><xsl:value-of select="@tests"/>
			<xsl:text>, Failures: </xsl:text><xsl:value-of select="@failures"/>
			<xsl:text>, Errors: </xsl:text><xsl:value-of select="@errors"/>
			<xsl:text>, Skipped: </xsl:text><xsl:value-of select="@skipped"/>
			<xsl:text>, Time elapsed: </xsl:text><xsl:value-of select="@time"/>
			<xsl:text> sec&#x000a;</xsl:text>
	</xsl:template>

	<xsl:template name="lastPathComponent">
		<xsl:param name="path"/>
		<xsl:choose>
			<xsl:when test="contains($path, '/')">
				<xsl:call-template name="lastPathComponent">
					<xsl:with-param name="path" select="substring-after($path, '/')"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$path"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:transform>
