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
			.main{ behavior:url(#default#savehistory);
						width:930px;}
			.main h3 { background-color:#FFFFFF;
						width:841px;
						margin-right:0px;
						font-size:16px;}
			.main h4 { background-color:#CCCCCC;
						width:860px;
						margin-left:5px;
						margin-right:0px}
			a.typeslnk{font-family:Arial, Helvetica, sans-serif;
					   	text-decoration:none;
					   	color:#000000;
					   	margin-left:1.5em;}
			a.typeslnk:hover{text-decoration:underline;}
			a.typeslnk span.typespan{font-family:Arial, Helvetica, sans-serif;
   					                 	font-weight:normal;}
			a.kindslnk{font-family:Arial, Helvetica, sans-serif;
					  	text-decoration:none;
					   	color:#000000;
					   	margin-left:1.5em;}
			.types{display:none;
					border:thin;
					margin-bottom:5px;
					width:800;
					margin-top:0px;
					margin-right:1.5em;
					border-style:solid;
				   	margin-left:1.5em;} 
			.kinds{display:none;
					width:800px;
					margin-bottom:5px;
					margin-top:0px;
					margin-right:0px;
				   	margin-left:2.0em;}
		</style>
		<script type="text/javascript">
			function twistie(loc){
			   if(document.getElementById){
				  var foc = loc.firstChild;
				  foc = loc.firstChild.innerHTML ? loc.firstChild : loc.firstChild.nextSibling;
				  foc.innerHTML = foc.innerHTML == '+'?'-':'+';
				  foc = loc.parentNode.nextSibling.style ? loc.parentNode.nextSibling : loc.parentNode.nextSibling.nextSibling;
				  foc.style.display = foc.style.display == 'block' ? 'none' : 'block';
				}
			}  
			if(!document.getElementById)
			   document.write('<style type="text/css"><!--\n'+
				  '.types{display:block;\n'+
				  '.kinds{display:block;}\n'+
				  '//--></style>');
		</script>
		<noscript>
		<style type="text/css">
			.types{display:block;}
			.kinds{display:block;}
		</style>
		</noscript>
		</head><body>
		<h1>Reference Details</h1>
		<div align="left" class="main">
		<table border="1" width="100%">
			<tr>
				<td><strong>Referenced Type Name</strong></td>
			</tr>
					<xsl:for-each select="references/type_name">
					<tr>
						<td>
							<h3>
								<a href="javascript:void(0)" class="typeslnk" onclick="twistie(this)">
									<span class="typespan">+</span><xsl:value-of disable-output-escaping="yes" select="@name"></xsl:value-of>
								</a>
							</h3>
							<div class="types">
								<xsl:for-each select="reference_kind">
								<xsl:sort select="@reference_kind_name"/>
								<h4><a href="javascript:void(0)" class="kindslnk" onclick="twistie(this)">
									<span class="typespan">+</span><u><xsl:value-of disable-output-escaping="yes" select="@reference_kind_name"></xsl:value-of></u></a></h4>
									<div class="kinds">
										<table border="1" width="100%">
											<tr bgcolor="#CC9933">
												<td><b>Reference Location</b></td>
												<td align="center"><b>Line Number</b></td>
											</tr>
											<xsl:for-each select="reference">
												<xsl:sort select="@origin"/>
												<tr align="left">
													<td><xsl:value-of disable-output-escaping="yes" select="@origin"></xsl:value-of></td>
													<td align="center"><xsl:value-of disable-output-escaping="yes" select="@linenumber"></xsl:value-of></td>
												</tr>
											</xsl:for-each>
										</table>
									</div>
								</xsl:for-each>
							</div>
						</td>
					</tr>
					</xsl:for-each>
		</table>
		</div>
		</body>
	</html>
	</xsl:template>
</xsl:stylesheet>
