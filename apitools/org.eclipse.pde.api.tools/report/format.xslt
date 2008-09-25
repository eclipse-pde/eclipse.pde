<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" indent="yes"/>

<xsl:template match="/versionReport">
	<html>
	<head>
		<link rel="stylesheet" type="text/css" href="style.css"/>
		<title><xsl:value-of select="@title"/></title>
	</head>
	<body>
		<div class="pageTitle"><xsl:value-of select="@title"/></div>
		
 		<xsl:variable name="majorContainers" select="container[@error='major_version']"/>
  		<xsl:if test="$majorContainers">
			<div class="modifiedBuckets">The following containers made non-backward compatible API changes and might have to increment MAJOR versions:</div>
			<ul>
				<xsl:for-each select="$majorContainers">
					<xsl:sort select="@name"/>
					<li><xsl:value-of select="@name"/></li>
				</xsl:for-each>
			</ul>
			<p></p>
  		</xsl:if>
		
 		<xsl:variable name="minorContainers" select="container[@error='minor_version']"/>
  		<xsl:if test="$minorContainers">
			<div class="modifiedBuckets">The following containers made backward compatible changes and might have to increment MINOR versions:</div>
			<ul>
				<xsl:for-each select="$minorContainers">
					<xsl:sort select="@name"/>
					<li><xsl:value-of select="@name"/></li>
				</xsl:for-each>
			</ul>
			<p></p>
  		</xsl:if>

		
 		<xsl:variable name="qualifierContainers" select="container[@error='micro_version']"/>
  		<xsl:if test="$qualifierContainers">
			<div class="modifiedBuckets">The following containers changed implementation and might have to increment MICRO versions:</div>
			<ul>
				<xsl:for-each select="$qualifierContainers">
					<xsl:sort select="@name"/>
					<li><xsl:value-of select="@name"/></li>
				</xsl:for-each>
			</ul>
			<p></p>
  		</xsl:if>
 	</body>
	</html>
</xsl:template>

<xsl:template match="/apiSnapshot">
	<html>
	<head>
		<link rel="stylesheet" type="text/css" href="style.css"/>
		<title><xsl:value-of select="@title"/></title>
	</head>
	<body>
		<div class="pageTitle"><xsl:value-of select="@title"/></div>
		
 		<xsl:variable name="removedContainers" select="container[@compare='removed']"/>
  		<xsl:if test="$removedContainers">
			<div class="modifiedBuckets">Removed Containers</div>
			<ul>
				<xsl:for-each select="$removedContainers">
					<xsl:sort select="@name"/>
					<li><xsl:value-of select="@name"/></li>
				</xsl:for-each>
			</ul>
			<p></p>
  		</xsl:if>
		
 		<xsl:variable name="addedContainers" select="container[@compare='added']"/>
  		<xsl:if test="$addedContainers">
			<div class="modifiedBuckets">Added Containers</div>
			<ul>
				<xsl:for-each select="$addedContainers">
					<xsl:sort select="@name"/>
					<li><xsl:value-of select="@name"/></li>
				</xsl:for-each>
			</ul>
			<p></p>
  		</xsl:if>
		
		<xsl:variable name="modifiedContainers" select="container[not(@compare)]"/>
  		<xsl:if test="$modifiedContainers">
				<xsl:apply-templates select = "$modifiedContainers">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
  		</xsl:if>
  		
 	</body>
	</html>
</xsl:template>

<xsl:template match="container">
		<div class="bucketHeader">Container: <xsl:value-of select="@name"/></div>
		
 		<xsl:variable name="removedInterfaces" select="interface[@compare='removed']"/>
  		<xsl:if test="$removedInterfaces">
			<div class="apiHeader">Removed Interfaces</div>
			<table class="apiTable" width="100%">
				<tr><td><ul>
				<xsl:for-each select="$removedInterfaces">
					<xsl:sort select="@aName"/>
					<li><xsl:value-of select="@aName"/></li>
				</xsl:for-each>
				</ul></td></tr>
			</table>
			<p></p>
  		</xsl:if>

		<xsl:variable name="removedClasses" select="class[@compare='removed']"/>
  		<xsl:if test="$removedClasses">
			<div class="apiHeader">Removed Classes</div>
			<table class="apiTable" width="100%">
				<tr><td><ul>
				<xsl:for-each select="$removedClasses">
					<xsl:sort select="@aName"/>
					<li><xsl:value-of select="@aName"/></li>
				</xsl:for-each>
				</ul></td></tr>
			</table>
			<p></p>
  		</xsl:if>

		<xsl:variable name="addedInterfaces" select="interface[@compare='added']"/>
  		<xsl:if test="$addedInterfaces">
			<div class="apiHeader">Added Interfaces</div>
			<table class="apiTable" width="100%">
				<tr><td><ul>
				<xsl:for-each select="$addedInterfaces">
					<xsl:sort select="@aName"/>
					<li><xsl:value-of select="@aName"/></li>
				</xsl:for-each>
				</ul></td></tr>
			</table>
			<p></p>
  		</xsl:if>
 		
		<xsl:variable name="addedClasses" select="class[@compare='added']"/>
  		<xsl:if test="$addedClasses">
			<div class="apiHeader">Added Classes</div>
			<table class="apiTable" width="100%">
				<tr><td><ul>
				<xsl:for-each select="$addedClasses">
					<xsl:sort select="@aName"/>
					<li><xsl:value-of select="@aName"/></li>
				</xsl:for-each>
				</ul></td></tr>
			</table>
			<p></p>
  		</xsl:if>

		<xsl:variable name="modifiedInterfaces" select="interface[not(@compare)]"/>
  		<xsl:if test="$modifiedInterfaces">
  			<div class="apiHeader">Modified Interfaces</div>
		 	<table class="apiTable" width="100%">
				<xsl:apply-templates select = "$modifiedInterfaces">
					<xsl:sort select="@aName"/>
				</xsl:apply-templates>
			</table>
			<p></p>
  		</xsl:if>
		
		<xsl:variable name="modifiedClasses" select="class[not(@compare)]"/>
  		<xsl:if test="$modifiedClasses">
  			<div class="apiHeader">Modified Classes</div>
		 	<table class="apiTable" width="100%">
				<xsl:apply-templates select = "$modifiedClasses">
					<xsl:sort select="@aName"/>
				</xsl:apply-templates>
			</table>
			<p></p>
  		</xsl:if>
</xsl:template>

<xsl:template name="listAPIs">
	<xsl:param name = "header"/>
	<xsl:param name = "list"/>
	<div class="apiHeader"><xsl:value-of select="$header"/></div>
	<table class="apiTable" width="100%">
		<tr><td><ul>
		<xsl:for-each select="$list">
			<li><xsl:value-of select="@aName"/></li>
		</xsl:for-each>
		</ul></td></tr>
	</table>
	<p></p>
</xsl:template>

<xsl:template match="class | interface">
	<tr>
		<td class="apiBlock" width="100%" colspan="5"><xsl:value-of select="@aName"/></td>

		<xsl:if test="@properties">
			<tr>
				<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
				<td width="35%" colspan="4">
					<div class="apiSubBlock">Properties: </div><xsl:value-of select="@properties"/>
				</td>
			</tr>
		</xsl:if>

		<xsl:if test="@extends">
			<tr>
				<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
				<td width="35%" colspan="4">
					<div class="apiSubBlock">Extends: </div><xsl:value-of select="@extends"/>
				</td>
			</tr>
		</xsl:if>

		<xsl:if test="@impl">
			<tr>
				<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
				<td width="35%" colspan="4">
					<div class="apiSubBlock">Implements: </div><xsl:value-of select="@impl"/>
				</td>
			</tr>
		</xsl:if>
		
	</tr>
	<xsl:if test="field">
		<tr>
			<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
			<td class="apiSubBlock" width="35%" colspan="2">Fields</td>
			<td class="apiGroupHeader" width="40%">Type</td>
			<td class="apiGroupHeader" width="20%">Comments</td>
			
		</tr>
		<xsl:apply-templates select="field">
			<xsl:sort select="@aName"/>
		</xsl:apply-templates>
	</xsl:if>
	<xsl:if test="method">
		<tr>
			<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
			<td class="apiSubBlock" width="35%" colspan="2">Methods</td>
			<td class="apiGroupHeader" width="40%">Signature</td>
			<td class="apiGroupHeader" width="20%">Comments</td>
		</tr>
		<xsl:apply-templates select="method">
			<xsl:sort select="@aName"/>
		</xsl:apply-templates>
	</xsl:if>
</xsl:template>

<xsl:template match="method">
	<tr>
	<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
	<td class="compare" width="5%"><xsl:value-of select="@compare"/></td>
	<td width="30%"><xsl:value-of select="@aName"/></td>
	<td width="40%"><xsl:value-of select="@type"/></td>
	<td width="20%">
		<xsl:value-of select="@properties"/>
		<xsl:if test="@throws">
			<div>Throws: <xsl:value-of select="@throws"/></div>
		</xsl:if>
	</td>
	</tr>
</xsl:template>

<xsl:template match="field">
	<tr>
	<td width="5%"><xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text></td>
	<td class="compare" width="5%"><xsl:value-of select="@compare"/></td>
	<td width="30%"><xsl:value-of select="@aName"/></td>
	<td width="40%"><xsl:value-of select="@type"/></td>
	<td width="20%">
		<xsl:value-of select="@properties"/>
		<xsl:if test="@value">
			<div>Default: <xsl:value-of select="@value"/></div>
		</xsl:if>
	</td>
	</tr>
</xsl:template>


</xsl:stylesheet>
