<?xml version="1.0" encoding="iso-8859-1"?>
<!--
	Copyright (c) IBM Corporation and others 2009, 2011. This page is made available under license. For full details see the LEGAL in the documentation book that contains this page.
	
	All Platform Debug contexts, those for org.eclipse.debug.ui, are located in this file
	All contexts are grouped by their relation, with all relations grouped alphabetically.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="iso-8859-1" doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<title>List of bundles that were not searched</title>
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
		<h3>
			Bundles that were not searched
		</h3>
			<xsl:choose>
				<xsl:when test="count(components/component) &gt; 0">
				<xsl:variable name="ShowMissing" select="components/@ShowMissing" />
				<xsl:if test="$ShowMissing != 'false'">
				<p>
					A summary of the missing required bundles is <a href="missing.html">available here</a>.
				</p>
				</xsl:if>
				<p>
					Click an entry in the table below to reveal the details of why it was not searched.
				</p>
				<div align="left" class="main">
					<table border="1" width="60%">
						<tr bgcolor="#E0C040">
							<td><b>Skipped Bundles</b></td>
						</tr>
						<xsl:for-each select="components/component">
							<xsl:sort select="@id"/>
							<tr>
								<td>
									<h3>
										<b>
											<a href="javascript:void(0)" class="typeslnk" onclick="expand(this)">
												<span>[+] </span><xsl:value-of disable-output-escaping="yes" select="@id"/> (<xsl:value-of disable-output-escaping="yes" select="@version"/>)
											</a>
										</b>
									</h3>
									<div class="types">
										<table border="0" width="100%">
											<tr>
												<td bgcolor="#CCCCCC"><b>Details</b></td>
											</tr>	
											<tr align="left">
												<pre>
													<td><xsl:value-of disable-output-escaping="yes" select="@details"/></td>
												</pre>
											</tr>
										</table>
									</div>
								</td>
							</tr>
						</xsl:for-each>
					</table>
					</div>
				</xsl:when>
				<xsl:otherwise>
					<p>No bundles were skipped during the search.</p>
				</xsl:otherwise>
			</xsl:choose>
			<p>
				<a href="index.html">Back to bundle summary</a>
			</p>
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
