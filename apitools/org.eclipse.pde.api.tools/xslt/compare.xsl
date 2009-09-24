<?xml version="1.0" encoding="iso-8859-1"?>
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
		<meta http-equiv="Content-Script-Type" content="text/javascript"/>
		<meta name="copyright" content="Copyright (c) IBM Corporation and others 2009. This page is made available under license. For full details see the LEGAL in the documentation book that contains this page."/>
  		<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
  		<meta http-equiv="Content-Style-Type" content="text/css"/>
		<title>Compare Details</title>
		<style type="text/css">
			.main{		font-family:Arial;
						Helvetica, sans-serif;
						text-align: left;}
			.main h3 {	font-family:Arial, Helvetica, sans-serif;
						background-color:#FFFFFF;
						font-size:16px;
						margin:0.1em;}
			.main h4 { 	background-color:#CCCCCC;
						margin:0.15em;}
			a.typeslnk{	font-family:Arial, Helvetica, sans-serif;
					   	text-decoration:none;}
			a.typeslnk:hover{text-decoration:underline;}
			.types{	display:none;
					margin-bottom:0.25em;
					margin-top:0.25em;
					margin-right:0.25em;
				   	margin-left:0.75em;}
			thead.head{background-color:#CC9933}
		</style>
		<script type="text/javascript">
			function expand(location){
			   if(document.getElementById){
				  var childhtml = location.firstChild;
				  if(!childhtml.innerHTML) {
				  	childhtml = childhtml.nextSibling;
				  }
				  childhtml.innerHTML = childhtml.innerHTML == '[+] ' ? '[-] ' : '[+] ';
				  var parent = location.parentNode;
				  childhtml = parent.nextSibling.style ? parent.nextSibling : parent.nextSibling.nextSibling;
				  childhtml.style.display = childhtml.style.display == 'block' ? 'none' : 'block';
				}
			}  
		</script>
		
	</head>
	<body>
		<noscript>
			<style type="text/css">
				.types{display:block;}
				.kinds{display:block;}
			</style>
		</noscript>
		<h1>Compare Details</h1>
		<div class="main">
			<xsl:variable name="breaking" select="deltas/delta[@compatible='false']"/>
			<xsl:choose>
				<xsl:when test="count($breaking) &gt; 0">
					<table border="0" width="60%">
						<tr>
							<td>
								<h3>
								<a href="javascript:void(0)" class="typeslnk" onclick="expand(this)">
									<span>[+] </span> List of breaking changes
								</a>
								</h3>
								<div class="types">
									<table border="1" width="100%" style="line">
										<thead class="head">
											<tr><td><b>Changes</b></td></tr>
										</thead>
										<xsl:for-each select="$breaking">
											<tr><td><xsl:value-of disable-output-escaping="yes" select="@message"/></td></tr>
										</xsl:for-each>
									</table>
								</div>
							</td>
						</tr>
					</table>
				</xsl:when>
				<xsl:otherwise>
					<h3>There are no breaking changes.</h3>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:variable name="compatible" select="deltas/delta[@compatible='true']"/>
			<xsl:choose>
				<xsl:when test="count($compatible) &gt; 0">
				<table border="0" width="60%">
					<tr>
						<td>
							<h3>
							<a href="javascript:void(0)" class="typeslnk" onclick="expand(this)">
								<span>[+] </span> List of compatible changes
							</a>
							</h3>
							<div class="types">
								<table border="1" width="100%" style="line">
									<thead class="head">
										<tr><td><b>Changes</b></td></tr>
									</thead>
									<xsl:for-each select="$compatible">
										<tr><td><xsl:value-of disable-output-escaping="yes" select="@message"/></td></tr>
									</xsl:for-each>
								</table>
							</div>
						</td>
					</tr>
				</table>
				</xsl:when>
				<xsl:otherwise>
					<h3>There are no compatible changes.</h3>
				</xsl:otherwise>
			</xsl:choose>
		</div>
		<p>
			<a href="http://validator.w3.org/check?uri=referer">
				<img src="http://www.w3.org/Icons/valid-xhtml10-blue" alt="Valid XHTML 1.0 Strict" height="31" width="88"/>
			</a>
		</p>
	</body>
</html>
</xsl:template>
</xsl:stylesheet>
