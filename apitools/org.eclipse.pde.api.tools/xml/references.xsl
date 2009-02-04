<?xml version="1.0" encoding="iso-8859-1"?>
<!--
	Copyright (c) IBM Corporation and others 2009. This page is made available under license. For full details see the LEGAL in the documentation book that contains this page.
	
	All Platform Debug contexts, those for org.eclipse.debug.ui, are located in this file
	All contexts are grouped by their relation, with all relations grouped alphabetically.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="iso-8859-1" doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
<xsl:template match="/">
	<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<title>Reference Details</title>
		</head>
		<body>
			<h1>Reference Details</h1>	
				<table align="left" border="1" cols="3" width="80%">
				<xsl:for-each select="references/reference_kind">
					<tr align="left" bgcolor="#CC9933">
						<td colspan="3"><b><xsl:value-of disable-output-escaping="yes" select="@reference_kind_name"></xsl:value-of></b></td>
					</tr>
					<tr>
						<td><b>Referee</b></td>
						<td><b>Origin</b></td>
						<td align="center"><b>Line Number</b></td>
					</tr>
					<xsl:for-each select="reference">
					<xsl:sort select="@referee"/>
						<tr align="left">
							<td><xsl:value-of disable-output-escaping="yes" select="@referee"></xsl:value-of></td>
							<td><xsl:value-of disable-output-escaping="yes" select="@origin"></xsl:value-of></td>
							<td align="center"><xsl:value-of disable-output-escaping="yes" select="@linenumber"></xsl:value-of></td>
						</tr>
					</xsl:for-each>
				</xsl:for-each>
				</table>
			<hr/>
		</body>
	</html>
</xsl:template>
</xsl:stylesheet>
