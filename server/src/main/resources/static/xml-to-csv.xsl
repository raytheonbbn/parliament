<?xml version="1.0" encoding="utf-8"?>

<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:sr="http://www.w3.org/2005/sparql-results#">
	<!--xsl:output method="text" encoding="iso-8859-1"/-->
	<xsl:output method="text" encoding="utf-8"/>

	<xsl:template match="/">
		<xsl:apply-templates select="sr:sparql/sr:head/sr:variable"/>
		<xsl:apply-templates select="sr:sparql/sr:results/sr:result"/>
	</xsl:template>

	<xsl:template match="sr:variable[position() != last()]">
		<xsl:call-template name="QuoteColumnValue">
			<xsl:with-param name="text" select="@name"/>
		</xsl:call-template>
		<xsl:text>,</xsl:text>
	</xsl:template>

	<xsl:template match="sr:variable[position() = last()]">
		<xsl:call-template name="QuoteColumnValue">
			<xsl:with-param name="text" select="@name"/>
		</xsl:call-template>
		<xsl:text>&#xA;</xsl:text>
	</xsl:template>

	<xsl:template match="sr:result">
		<xsl:variable name="currentResultNode" select="."/>
		<xsl:for-each select="/sr:sparql/sr:head/sr:variable">
			<xsl:call-template name="ProcessResultColumn">
				<xsl:with-param name="resultNode" select="$currentResultNode"/>
				<xsl:with-param name="columnName" select="@name"/>
				<xsl:with-param name="isLastColumn" select="position() = last()"/>
			</xsl:call-template>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="ProcessResultColumn">
		<xsl:param name="resultNode"/>
		<xsl:param name="columnName"/>
		<xsl:param name="isLastColumn"/>
		<xsl:apply-templates select="$resultNode/sr:binding[@name = $columnName]">
			<xsl:with-param name="isLastColumn" select="$isLastColumn"/>
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="sr:binding">
		<xsl:param name="isLastColumn"/>
		<xsl:apply-templates select="sr:uri|sr:bnode|sr:literal"/>
		<xsl:choose>
			<xsl:when test="$isLastColumn">
				<xsl:text>&#xA;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>,</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="sr:literal[@datatype != '']">
		<xsl:call-template name="QuoteColumnValue">
			<xsl:with-param name="text">
				<xsl:text>"</xsl:text>
				<xsl:value-of select="."/>
				<xsl:text>"^^</xsl:text>
				<xsl:value-of select="@datatype"/>
			</xsl:with-param>
	</xsl:call-template>
	</xsl:template>

	<xsl:template match="sr:literal[@xml:lang != '']">
		<xsl:call-template name="QuoteColumnValue">
			<xsl:with-param name="text">
				<xsl:text>"</xsl:text>
				<xsl:value-of select="."/>
				<xsl:text>"@</xsl:text>
				<xsl:value-of select="translate(@xml:lang, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="sr:uri|sr:bnode|sr:literal">
		<xsl:call-template name="QuoteColumnValue">
			<xsl:with-param name="text" select="."/>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="QuoteColumnValue">
		<xsl:param name="text"/>
		<xsl:choose>
			<xsl:when test="contains($text,'&#x22;')">
				<xsl:text>"</xsl:text>
				<xsl:call-template name="StringReplaceAll">
					<xsl:with-param name="text" select="$text"/>
					<xsl:with-param name="pattern">"</xsl:with-param>
					<xsl:with-param name="replacement">""</xsl:with-param>
				</xsl:call-template>
				<xsl:text>"</xsl:text>
			</xsl:when>
			<xsl:when test="contains($text,',') or contains($text,'&#xD;') or contains($text,'&#xA;') or starts-with($text,' ') or starts-with($text,'&#x9;') or substring($text,string-length($text)) = ' ' or substring($text,string-length($text)) = '&#x9;'">
				<xsl:text>"</xsl:text>
				<xsl:value-of select="$text"/>
				<xsl:text>"</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="StringReplaceAll">
		<xsl:param name="text"/>
		<xsl:param name="pattern"/>
		<xsl:param name="replacement"/>
		<xsl:choose>
			<xsl:when test="contains($text, $pattern)">
				<xsl:value-of select="substring-before($text,$pattern)"/>
				<xsl:value-of select="$replacement"/>
				<xsl:call-template name="StringReplaceAll">
					<xsl:with-param name="text" select="substring-after($text,$pattern)"/>
					<xsl:with-param name="pattern" select="$pattern"/>
					<xsl:with-param name="replacement" select="$replacement"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:transform>
