<?xml version="1.0" encoding="UTF-8"?>

<xsl:transform version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">

	<xsl:output method="text"/>

	<xsl:template match="/">
		<xsl:call-template name="printHeaders"/>
		<xsl:apply-templates
			select="ivy-report/dependencies/module/revision[not(@evicted)]"/>
	</xsl:template>

	<xsl:template match="revision">
		<xsl:text>"</xsl:text>
		<xsl:value-of select="../@organisation"/>
		<xsl:text>","</xsl:text>
		<xsl:value-of select="../@name"/>
		<xsl:text>","</xsl:text>
		<xsl:value-of select="@name"/>
		<xsl:text>","</xsl:text>
		<xsl:value-of select="license/@name"/>
		<xsl:text>","</xsl:text>
		<xsl:value-of select="artifacts/artifact/origin-location/@location"/>
		<xsl:text>"&#x000a;</xsl:text>
	</xsl:template>

	<xsl:template name="printHeaders">
		<xsl:text>.groupId,module,version,license,url&#x000a;</xsl:text>
	</xsl:template>
</xsl:transform>
