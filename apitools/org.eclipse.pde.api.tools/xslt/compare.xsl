<?xml version="1.0" encoding="iso-8859-1"?>
<!--
	Copyright (c) IBM Corporation and others 2009. This page is made available under license. For full details see the LEGAL in the documentation book that contains this page.
	
	All Platform Debug contexts, those for org.eclipse.debug.ui, are located in this file
	All contexts are grouped by their relation, with all relations grouped alphabetically.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output
               xmlns="http://www.w3.org/1999/xhtml"
               method="xml"
               encoding="ISO-8859-1"
               media-type="text/html"
               doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
               doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
               indent="yes"/>
<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
	<head>
		<title>Compare Details</title>
		<style type="text/css">
			table {
				width: 70%;
				border-style: solid;
				border-width: 1px;
				border-color: #666666;
				border-collapse: collapse;
			}
			tbody {
				display:block
			}
			td {
				border-style: solid;
				border-width: 1px;
				border-color: #666666;
			}
			a {
				text-decoration: none;
				font-weight: bold;
			}
			a.typeslnk {
				color:black;
				font-family:Arial, Helvetica, sans-serif;
				text-decoration:none;
				margin-left:0.25em;
			}
			a.typeslnk:hover{
				text-decoration:underline;
			}
			.link {
				color:black;
			}
			.vis {
				display:block;
			}
			.vis tr {
					border-style: solid;
					border-width: 1px;
					border-color: #666666;
			}
			.vis th {
					border-style: solid;
					border-width: 1px;
					border-color: #666666;
					background-color:#CC9933
			}
		</style>
		<script type="text/javascript">
		<xsl:text disable-output-escaping="yes">&lt;</xsl:text>!-- Begin
			var element=false;
			function hideall() {
				var tBodyElements = document.getElementsByTagName('tbody');
				for (i=0;i <xsl:text disable-output-escaping="yes">&lt;</xsl:text> tBodyElements.length;i++) {
					tBodyElements[i].style.display='none';
				}
			}

			function showHide(tBodyID,link) {
				element=document.getElementById(tBodyID);
				if (element.style.display=='none') {
					document.getElementById(link).innerHTML=' - ';
					element.style.display='block';
				} else {
					document.getElementById(link).innerHTML=' + ';
					element.style.display='none';
				}
			}
			onload=hideall;
		// End --<xsl:text disable-output-escaping="yes">&gt;</xsl:text>
		</script>
	</head>
	<body>
		<h1>Compare Details</h1>
		<div align="left">
			<xsl:apply-templates select="deltas"/>
		</div>
		<p>
			<a href="http://validator.w3.org/check?uri=referer">
				<img src="http://www.w3.org/Icons/valid-xhtml10-blue" alt="Valid XHTML 1.0 Strict" height="31" width="88"/>
			</a>
		</p>
	</body>
</html>
</xsl:template>
<xsl:template match="deltas">
	<table>
		<thead>
			<tr class="vis">
				<th class="vis">
					<a href="javascript:void(0)" class="typeslnk" onclick="showHide('breaking', 'breakingspan')">
						<span id="breakingspan" class="link" > + </span> List of breaking changes
					</a>
				</th>
			</tr>
		</thead>
		<tbody id="breaking">
			<xsl:for-each select="delta">
				<xsl:if test="(@compatible='false')">
					<tr>
						<td>
							<xsl:value-of disable-output-escaping="yes" select="@message"/>
						</td>
					</tr>
				</xsl:if>
			</xsl:for-each>
		</tbody>
	</table>
	<p></p>
	<table>
		<thead>
			<tr class="vis">
				<th class="vis">
					<a href="javascript:void(0)" class="typeslnk" onclick="showHide('compatible', 'compatiblespan')">
						<span id="compatiblespan" class="link"> + </span> List of compatible changes
					</a>
				</th>
			</tr>
		</thead>
		<tbody id="compatible">
			<xsl:for-each select="delta">
				<xsl:if test="(@compatible='true')">
					<tr>
						<td>
							<xsl:value-of disable-output-escaping="yes" select="@message"/>
						</td>
					</tr>
				</xsl:if>
			</xsl:for-each>
		</tbody>
	</table>
</xsl:template>
</xsl:stylesheet>
