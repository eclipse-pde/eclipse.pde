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
		<style type="text/css">
			.main{		font-family:Arial, Helvetica, sans-serif;}
			.main h3 {	font-family:Arial, Helvetica, sans-serif;
						background-color:#FFFFFF;
						font-size:14px;
						margin:0.1em;}
			.main h4 { 	background-color:#CCCCCC;
						margin:0.15em;}
			a.typeslnk{	font-family:Arial, Helvetica, sans-serif;
					   	text-decoration:none;
					   	margin-left:0.25em;}
			a.typeslnk:hover{text-decoration:underline;}
			a.kindslnk{	font-family:Arial, Helvetica, sans-serif;
					  	text-decoration:none;
					   	margin-left:0.25em;}
			.types{	display:none;
					margin-bottom:0.25em;
					margin-top:0.25em;
					margin-right:0.25em;
				   	margin-left:0.75em;} 
		</style>
		<script type="text/javascript">
			function expand(location){
			   if(document.getElementById){
				 var childhtml = location.firstChild;
				  if(!childhtml.innerHTML) {
				  	childhtml = childhtml.nextSibling;
				  }
				  childhtml.innerHTML = childhtml.innerHTML == '[+] ' ? '[-] ' : '[+] ';
				  var parent = location.parentNode.parentNode;
				  childhtml = parent.nextSibling.style ? parent.nextSibling : parent.nextSibling.nextSibling;
				  childhtml.style.display = childhtml.style.display == 'block' ? 'none' : 'block';
				}
			}  
		</script>
		<noscript>
			<style type="text/css">
				.types{display:block;}
				.kinds{display:block;}
			</style>
		</noscript>
	</head>
	<body>
		<xsl:variable name="originbundle" select="references/@origin"/>
		<h3>
			<xsl:value-of select="references/@name"/> from <xsl:value-of select="references/@referee"/> used by <xsl:value-of  select="$originbundle"/>
		</h3>
		<p>
			Click an entry in the table below to reveal the details of the references made to that element.
		</p>
		<div align="left" class="main">
			<table border="1" width="90%">
				<tr bgcolor="#E0C040">
					<td><b>Reference Details</b></td>
				</tr>
				<xsl:for-each select="references/target">
				<xsl:sort select="@name"/>
					<tr>
						<td>
							<h3>
								<b>
									<a href="javascript:void(0)" class="typeslnk" onclick="expand(this)">
										<span>[+] </span><xsl:value-of disable-output-escaping="yes" select="@name"/>
									</a>
								</b>
							</h3>
							<div class="types">
								<table border="0" width="100%">
									<xsl:for-each select="reference_kind">
										<xsl:sort select="@reference_kind_name"/>
										<tr>
											<td colspan="2" bgcolor="#CCCCCC"><b><xsl:value-of select="@reference_kind_name"/></b></td>
										</tr>	
										<tr bgcolor="#CC9933">
											<td align="left" width="92%"><b>Reference Location</b></td>
											<td align="center" width="8%"><b>Line Number</b></td>
										</tr>
										<xsl:for-each select="reference">
											<xsl:sort select="@origin"/>
											<tr align="left">
												<td><xsl:value-of disable-output-escaping="yes" select="@origin"/></td>
												<td align="center"><xsl:value-of select="@linenumber"/></td>
											</tr>
										</xsl:for-each>
									</xsl:for-each>
								</table>
							</div>
						</td>
					</tr>
				</xsl:for-each>
			</table>
			<p>
				Back to reference summary for <a href="../{$originbundle}.html"><xsl:value-of select="$originbundle"/></a>
			</p>
		</div>
		<p>
			<a href="http://validator.w3.org/check?uri=referer">
				<img src="http://www.w3.org/Icons/valid-html401-blue" alt="Valid HTML 4.01 Transitional" height="31" width="88" />
			</a>
			<a href="http://validator.w3.org/check?uri=referer">
				<img src="http://www.w3.org/Icons/valid-xhtml10-blue" alt="Valid XHTML 1.0 Strict" height="31" width="88" />
			</a>
		</p>
	</body>
</html>
</xsl:template>
</xsl:stylesheet>
