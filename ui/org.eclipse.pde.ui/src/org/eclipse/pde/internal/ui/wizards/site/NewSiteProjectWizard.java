/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.site;

import java.io.*;
import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.*;

public class NewSiteProjectWizard
	extends NewWizard
	implements IExecutableExtension {
	public static final String KEY_WTITLE = "NewSiteWizard.wtitle"; //$NON-NLS-1$
	public static final String MAIN_PAGE_TITLE = "NewSiteWizard.MainPage.title"; //$NON-NLS-1$
	public static final String CREATING_PROJECT =
		"NewSiteWizard.creatingProject"; //$NON-NLS-1$
	public static final String CREATING_FOLDERS =
		"NewSiteWizard.creatingFolders"; //$NON-NLS-1$
	public static final String CREATING_MANIFEST =
		"NewSiteWizard.creatingManifest"; //$NON-NLS-1$
	public static final String MAIN_PAGE_DESC = "NewSiteWizard.MainPage.desc"; //$NON-NLS-1$
	public static final String OVERWRITE_SITE = "NewFeatureWizard.overwriteSite"; //$NON-NLS-1$
	public static final String DEF_PROJECT_NAME ="project-name"; //$NON-NLS-1$

	private NewSiteProjectCreationPage mainPage;
	private IConfigurationElement config;
	private boolean createdProject = false;

	public NewSiteProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWSITEPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	public void addPages() {
		mainPage = new NewSiteProjectCreationPage("main"); //$NON-NLS-1$
		mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname!=null)
			mainPage.setInitialProjectName(pname);
		addPage(mainPage);
	}

	private IFile createSiteManifest(IProject project)
		throws CoreException {
		IFile file = project.getFile("site.xml"); //$NON-NLS-1$
		if (file.exists()) return file;
		WorkspaceSiteModel model = new WorkspaceSiteModel();
		model.setFile(file);
		ISite site = model.getSite();
		String name = project.getName();
		site.setLabel(name);

		// Save the model
		model.save();
		model.dispose();
		
		// Create and save build model
		WorkspaceSiteBuildModel buildModel = new WorkspaceSiteBuildModel();
		IFile buildFile = project.getFile(PDECore.SITEBUILD_FILE);
		buildModel.setFile(buildFile);
		ISiteBuild siteBuild = buildModel.getSiteBuild();
	 	siteBuild.setAutobuild(false);
	 	siteBuild.setShowConsole(true);
	 	buildModel.save();
		buildModel.dispose();
		
		// Set the default editor
		IDE.setDefaultEditor(file, PDEPlugin.SITE_EDITOR_ID);
		return file;
	}
	
	private void createHTMLFile(IProject project){
		try {
		IFile file = project.getFile("index.html"); //$NON-NLS-1$
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		
		writer.println("<html>"); //$NON-NLS-1$
		writer.println("<head>"); //$NON-NLS-1$
		writer.println("<title>"+project.getName()+"</title>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("<style>@import url(\""+mainPage.getWebLocation()+"/site.css\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("<script type=\"text/javascript\">"); //$NON-NLS-1$
		writer.println("	var returnval = 0;"); //$NON-NLS-1$
		writer.println("	var stylesheet, xmlFile, cache, doc;"); //$NON-NLS-1$
		writer.println("	function init(){"); //$NON-NLS-1$
		writer.println("		// NSCP 7.1+ / Mozilla 1.4.1+ / Safari"); //$NON-NLS-1$
		writer.println("		// Use the standard DOM Level 2 technique, if it is supported"); //$NON-NLS-1$
		writer.println("		if (document.implementation && document.implementation.createDocument) {"); //$NON-NLS-1$
		writer.println("			xmlFile = document.implementation.createDocument(\"\", \"\", null);"); //$NON-NLS-1$
		writer.println("			stylesheet = document.implementation.createDocument(\"\", \"\", null);"); //$NON-NLS-1$
		writer.println("			if (xmlFile.load){"); //$NON-NLS-1$
		writer.println("				xmlFile.load(\"site.xml\");"); //$NON-NLS-1$
		writer.println("				stylesheet.load(\""+mainPage.getWebLocation()+"/site.xsl\");"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("			} else {"); //$NON-NLS-1$
		writer.println("				alert(\"" + PDEPlugin.getResourceString("SiteHTML.loadError") + "\");"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println("			}"); //$NON-NLS-1$
		writer.println("			xmlFile.addEventListener(\"load\", transform, false);"); //$NON-NLS-1$
		writer.println("			stylesheet.addEventListener(\"load\", transform, false);"); //$NON-NLS-1$
		writer.println("		}"); //$NON-NLS-1$
		writer.println("		//IE 6.0+ solution"); //$NON-NLS-1$
		writer.println("		else if (window.ActiveXObject) {"); //$NON-NLS-1$
		writer.println("			xmlFile = new ActiveXObject(\"msxml2.DOMDocument.3.0\");"); //$NON-NLS-1$
		writer.println("			xmlFile.async = false;"); //$NON-NLS-1$
		writer.println("			xmlFile.load(\"site.xml\");"); //$NON-NLS-1$
		writer.println("			stylesheet = new ActiveXObject(\"msxml2.FreeThreadedDOMDocument.3.0\");"); //$NON-NLS-1$
		writer.println("			stylesheet.async = false;"); //$NON-NLS-1$
		writer.println("			stylesheet.load(\""+mainPage.getWebLocation()+"/site.xsl\");"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("			cache = new ActiveXObject(\"msxml2.XSLTemplate.3.0\");"); //$NON-NLS-1$
		writer.println("			cache.stylesheet = stylesheet;"); //$NON-NLS-1$
		writer.println("			transformData();"); //$NON-NLS-1$
		writer.println("		}"); //$NON-NLS-1$
		writer.println("	}"); //$NON-NLS-1$
		writer.println("	// separate transformation function for IE 6.0+"); //$NON-NLS-1$
		writer.println("	function transformData(){"); //$NON-NLS-1$
		writer.println("		var processor = cache.createProcessor();"); //$NON-NLS-1$
		writer.println("		processor.input = xmlFile;"); //$NON-NLS-1$
		writer.println("		processor.transform();"); //$NON-NLS-1$
		writer.println("		data.innerHTML = processor.output;"); //$NON-NLS-1$
		writer.println("	}"); //$NON-NLS-1$
		writer.println("	// separate transformation function for NSCP 7.1+ and Mozilla 1.4.1+ "); //$NON-NLS-1$
		writer.println("	function transform(){"); //$NON-NLS-1$
		writer.println("		returnval+=1;"); //$NON-NLS-1$
		writer.println("		if (returnval==2){"); //$NON-NLS-1$
		writer.println("			var processor = new XSLTProcessor();"); //$NON-NLS-1$
		writer.println("			processor.importStylesheet(stylesheet); "); //$NON-NLS-1$
		writer.println("			doc = processor.transformToDocument(xmlFile);"); //$NON-NLS-1$
		writer.println("			document.getElementById(\"data\").innerHTML = doc.documentElement.innerHTML;"); //$NON-NLS-1$
		writer.println("		}"); //$NON-NLS-1$
		writer.println("	}"); //$NON-NLS-1$
		writer.println("</script>"); //$NON-NLS-1$
		writer.println("</head>"); //$NON-NLS-1$
		writer.println("<body onload=\"init();\">"); //$NON-NLS-1$
		writer.println("<!--[insert static HTML here]-->"); //$NON-NLS-1$
		writer.println("<div id=\"data\"><!-- this is where the transformed data goes --></div>"); //$NON-NLS-1$
		writer.println("</body>"); //$NON-NLS-1$
		writer.println("</html>"); //$NON-NLS-1$

		writer.flush();
		swrite.close();
		ByteArrayInputStream stream = new ByteArrayInputStream(swrite.toString().getBytes("UTF8")); //$NON-NLS-1$
		if (file.exists()){
			file.setContents(stream, false, false, null);
		} else {
			file.create(stream, false, null);
		}
		stream.close();
		} catch (Exception e){
			PDEPlugin.logException(e);
		}
	}
		
	private void createCSSFile(IProject project){
		try {
		IFile file = project.getFile(mainPage.getWebLocation() + "/site.css"); //$NON-NLS-1$
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<STYLE type=\"text/css\">"); //$NON-NLS-1$
		writer.println("td.spacer {padding-bottom: 10px; padding-top: 10px;}"); //$NON-NLS-1$
		writer.println(".title { font-family: sans-serif; color: #99AACC;}"); //$NON-NLS-1$
		writer.println(".bodyText { font-family: sans-serif; font-size: 9pt; color:#000000;  }"); //$NON-NLS-1$
		writer.println(".sub-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white;}"); //$NON-NLS-1$
		writer.println(".log-text {font-family: sans-serif; font-style: normal; font-weight: lighter; font-size: 8pt; color:black;}"); //$NON-NLS-1$
		writer.println(".big-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white; border-top:10px solid white;}"); //$NON-NLS-1$
		writer.println(".light-row {background:#FFFFFF}"); //$NON-NLS-1$
		writer.println(".dark-row {background:#EEEEFF}"); //$NON-NLS-1$
		writer.println(".header {background:#99AADD}"); //$NON-NLS-1$
		writer.println("#indent {word-wrap : break-word;width :300px;text-indent:10px;}"); //$NON-NLS-1$
		writer.println("</STYLE>"); //$NON-NLS-1$

		writer.flush();
		swrite.close();
		ByteArrayInputStream stream = new ByteArrayInputStream(swrite.toString().getBytes("UTF8")); //$NON-NLS-1$
		if (file.exists()){
			file.setContents(stream, false, false, null);
		} else {
			file.create(stream, false, null);
		}
		stream.close();
		} catch (Exception e){
			PDEPlugin.logException(e);
		}
	}

	private void createXSLFile(IProject project){
		try {
		IFile file = project.getFile(mainPage.getWebLocation() + "/site.xsl"); //$NON-NLS-1$
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<xsl:stylesheet version = '1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns:msxsl=\"urn:schemas-microsoft-com:xslt\">"); //$NON-NLS-1$
		writer.println("<xsl:output method=\"html\" encoding=\"ISO-8859-1\"/>"); //$NON-NLS-1$
		writer.println("<xsl:key name=\"cat\" match=\"category\" use=\"@name\"/>"); //$NON-NLS-1$
		writer.println("<xsl:template match=\"/\">"); //$NON-NLS-1$
		writer.println("<xsl:for-each select=\"site\">"); //$NON-NLS-1$
		writer.println("	<html>"); //$NON-NLS-1$
		writer.println("	<head>"); //$NON-NLS-1$
		writer.println("	<title>"+project.getName()+"</title>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("	<style>@import url(\"" + mainPage.getWebLocation() + "/site.css\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("	</head>"); //$NON-NLS-1$
		writer.println("	<body>"); //$NON-NLS-1$
		writer.println("	<h1 class=\"title\">" + project.getName() +"</h1>"); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("	<p class=\"bodyText\"><xsl:value-of select=\"description\"/></p>"); //$NON-NLS-1$
		writer.println("	<table width=\"100%\" border=\"0\" cellspacing=\"1\" cellpadding=\"2\">"); //$NON-NLS-1$
		writer.println("	<xsl:for-each select=\"category-def\">"); //$NON-NLS-1$
		writer.println("		<xsl:sort select=\"@label\" order=\"ascending\" case-order=\"upper-first\"/>"); //$NON-NLS-1$
		writer.println("		<xsl:sort select=\"@name\" order=\"ascending\" case-order=\"upper-first\"/>"); //$NON-NLS-1$
		writer.println("	<xsl:if test=\"count(key('cat',@name)) != 0\">"); //$NON-NLS-1$
		writer.println("			<tr class=\"header\">"); //$NON-NLS-1$
		writer.println("				<td class=\"sub-header\" width=\"30%\">"); //$NON-NLS-1$
		writer.println("					<xsl:value-of select=\"@name\"/>"); //$NON-NLS-1$
		writer.println("				</td>"); //$NON-NLS-1$
		writer.println("				<td class=\"sub-header\" width=\"70%\">"); //$NON-NLS-1$
		writer.println("					<xsl:value-of select=\"@label\"/>"); //$NON-NLS-1$
		writer.println("				</td>"); //$NON-NLS-1$
		writer.println("			</tr>"); //$NON-NLS-1$
		writer.println("			<xsl:for-each select=\"key('cat',@name)\">"); //$NON-NLS-1$
		writer.println("			<xsl:sort select=\"ancestor::feature//@version\" order=\"ascending\"/>"); //$NON-NLS-1$
		writer.println("			<xsl:sort select=\"ancestor::feature//@id\" order=\"ascending\" case-order=\"upper-first\"/>"); //$NON-NLS-1$
		writer.println("			<tr>"); //$NON-NLS-1$
		writer.println("				<xsl:choose>"); //$NON-NLS-1$
		writer.println("				<xsl:when test=\"(position() mod 2 = 1)\">"); //$NON-NLS-1$
		writer.println("					<xsl:attribute name=\"class\">dark-row</xsl:attribute>"); //$NON-NLS-1$
		writer.println("				</xsl:when>"); //$NON-NLS-1$
		writer.println("				<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("					<xsl:attribute name=\"class\">light-row</xsl:attribute>"); //$NON-NLS-1$
		writer.println("				</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("				</xsl:choose>"); //$NON-NLS-1$
		writer.println("				<td class=\"log-text\" id=\"indent\">"); //$NON-NLS-1$
		writer.println("						<xsl:choose>"); //$NON-NLS-1$
		writer.println("						<xsl:when test=\"ancestor::feature//@label\">"); //$NON-NLS-1$
		writer.println("							<a href=\"{ancestor::feature//@url}\"><xsl:value-of select=\"ancestor::feature//@label\"/></a>"); //$NON-NLS-1$
		writer.println("							<br/>"); //$NON-NLS-1$
		writer.println("							<div id=\"indent\">"); //$NON-NLS-1$
		writer.println("							(<xsl:value-of select=\"ancestor::feature//@id\"/> - <xsl:value-of select=\"ancestor::feature//@version\"/>)"); //$NON-NLS-1$
		writer.println("							</div>"); //$NON-NLS-1$
		writer.println("						</xsl:when>"); //$NON-NLS-1$
		writer.println("						<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("						<a href=\"{ancestor::feature//@url}\"><xsl:value-of select=\"ancestor::feature//@id\"/> - <xsl:value-of select=\"ancestor::feature//@version\"/></a>"); //$NON-NLS-1$
		writer.println("						</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("						</xsl:choose>"); //$NON-NLS-1$
		writer.println("						<br />"); //$NON-NLS-1$
		writer.println("				</td>"); //$NON-NLS-1$
		writer.println("				<td>"); //$NON-NLS-1$
		writer.println("					<table>"); //$NON-NLS-1$
		writer.println("						<xsl:if test=\"ancestor::feature//@os\">"); //$NON-NLS-1$
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Operating Systems:</td>"); //$NON-NLS-1$
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@os\"/></td>"); //$NON-NLS-1$
		writer.println("							</tr>"); //$NON-NLS-1$
		writer.println("						</xsl:if>"); //$NON-NLS-1$
		writer.println("						<xsl:if test=\"ancestor::feature//@ws\">"); //$NON-NLS-1$
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Windows Systems:</td>"); //$NON-NLS-1$
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@ws\"/></td>"); //$NON-NLS-1$
		writer.println("							</tr>"); //$NON-NLS-1$
		writer.println("						</xsl:if>"); //$NON-NLS-1$
		writer.println("						<xsl:if test=\"ancestor::feature//@nl\">"); //$NON-NLS-1$
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Languages:</td>"); //$NON-NLS-1$
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@nl\"/></td>"); //$NON-NLS-1$
		writer.println("							</tr>"); //$NON-NLS-1$
		writer.println("						</xsl:if>"); //$NON-NLS-1$
		writer.println("						<xsl:if test=\"ancestor::feature//@arch\">"); //$NON-NLS-1$
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Architecture:</td>"); //$NON-NLS-1$
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@arch\"/></td>"); //$NON-NLS-1$
		writer.println("							</tr>"); //$NON-NLS-1$
		writer.println("						</xsl:if>"); //$NON-NLS-1$
		writer.println("					</table>"); //$NON-NLS-1$
		writer.println("				</td>"); //$NON-NLS-1$
		writer.println("			</tr>"); //$NON-NLS-1$
		writer.println("			</xsl:for-each>"); //$NON-NLS-1$
		writer.println("			<tr><td class=\"spacer\"><br/></td><td class=\"spacer\"><br/></td></tr>"); //$NON-NLS-1$
		writer.println("		</xsl:if>"); //$NON-NLS-1$
		writer.println("	</xsl:for-each>"); //$NON-NLS-1$
		writer.println("	<xsl:if test=\"count(feature)  &gt; count(feature/category)\">"); //$NON-NLS-1$
		writer.println("	<tr class=\"header\">"); //$NON-NLS-1$
		writer.println("		<td class=\"sub-header\" colspan=\"2\">"); //$NON-NLS-1$
		writer.println("		Uncategorized"); //$NON-NLS-1$
		writer.println("		</td>"); //$NON-NLS-1$
		writer.println("	</tr>"); //$NON-NLS-1$
		writer.println("	</xsl:if>"); //$NON-NLS-1$
		writer.println("	<xsl:choose>"); //$NON-NLS-1$
		writer.println("	<xsl:when test=\"function-available('msxsl:node-set')\">"); //$NON-NLS-1$
		writer.println("	   <xsl:variable name=\"rtf-nodes\">"); //$NON-NLS-1$
		writer.println("		<xsl:for-each select=\"feature[not(category)]\">"); //$NON-NLS-1$
		writer.println("			<xsl:sort select=\"@id\" order=\"ascending\" case-order=\"upper-first\"/>"); //$NON-NLS-1$
		writer.println("			<xsl:sort select=\"@version\" order=\"ascending\" />"); //$NON-NLS-1$
		writer.println("			<xsl:value-of select=\".\"/>"); //$NON-NLS-1$
		writer.println("			<xsl:copy-of select=\".\" />"); //$NON-NLS-1$
		writer.println("		</xsl:for-each>"); //$NON-NLS-1$
		writer.println("	   </xsl:variable>"); //$NON-NLS-1$
		writer.println("	   <xsl:variable name=\"myNodeSet\" select=\"msxsl:node-set($rtf-nodes)/*\"/>"); //$NON-NLS-1$
		writer.println("	<xsl:for-each select=\"$myNodeSet\">"); //$NON-NLS-1$
		writer.println("	<tr>"); //$NON-NLS-1$
		writer.println("		<xsl:choose>"); //$NON-NLS-1$
		writer.println("		<xsl:when test=\"position() mod 2 = 1\">"); //$NON-NLS-1$
		writer.println("		<xsl:attribute name=\"class\">dark-row</xsl:attribute>"); //$NON-NLS-1$
		writer.println("		</xsl:when>"); //$NON-NLS-1$
		writer.println("		<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("		<xsl:attribute name=\"class\">light-row</xsl:attribute>"); //$NON-NLS-1$
		writer.println("		</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("		</xsl:choose>"); //$NON-NLS-1$
		writer.println("		<td class=\"log-text\" id=\"indent\">"); //$NON-NLS-1$
		writer.println("			<xsl:choose>"); //$NON-NLS-1$
		writer.println("			<xsl:when test=\"@label\">"); //$NON-NLS-1$
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@label\"/></a>"); //$NON-NLS-1$
		writer.println("				<br />"); //$NON-NLS-1$
		writer.println("				<div id=\"indent\">"); //$NON-NLS-1$
		writer.println("				(<xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/>)"); //$NON-NLS-1$
		writer.println("				</div>"); //$NON-NLS-1$
		writer.println("			</xsl:when>"); //$NON-NLS-1$
		writer.println("			<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/></a>"); //$NON-NLS-1$
		writer.println("			</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("			</xsl:choose>"); //$NON-NLS-1$
		writer.println("			<br /><br />"); //$NON-NLS-1$
		writer.println("		</td>"); //$NON-NLS-1$
		writer.println("		<td>"); //$NON-NLS-1$
		writer.println("			<table>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@os\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Operating Systems:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@os\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@ws\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Windows Systems:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@ws\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@nl\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Languages:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@nl\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@arch\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Architecture:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@arch\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("			</table>"); //$NON-NLS-1$
		writer.println("		</td>"); //$NON-NLS-1$
		writer.println("	</tr>"); //$NON-NLS-1$
		writer.println("	</xsl:for-each>"); //$NON-NLS-1$
		writer.println("	</xsl:when>"); //$NON-NLS-1$
		writer.println("	<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("	<xsl:for-each select=\"feature[not(category)]\">"); //$NON-NLS-1$
		writer.println("	<xsl:sort select=\"@id\" order=\"ascending\" case-order=\"upper-first\"/>"); //$NON-NLS-1$
		writer.println("	<xsl:sort select=\"@version\" order=\"ascending\" />"); //$NON-NLS-1$
		writer.println("	<tr>"); //$NON-NLS-1$
		writer.println("		<xsl:choose>"); //$NON-NLS-1$
		writer.println("		<xsl:when test=\"count(preceding-sibling::feature[not(category)]) mod 2 = 1\">"); //$NON-NLS-1$
		writer.println("		<xsl:attribute name=\"class\">dark-row</xsl:attribute>"); //$NON-NLS-1$
		writer.println("		</xsl:when>"); //$NON-NLS-1$
		writer.println("		<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("		<xsl:attribute name=\"class\">light-row</xsl:attribute>"); //$NON-NLS-1$
		writer.println("		</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("		</xsl:choose>"); //$NON-NLS-1$
		writer.println("		<td class=\"log-text\" id=\"indent\">"); //$NON-NLS-1$
		writer.println("			<xsl:choose>"); //$NON-NLS-1$
		writer.println("			<xsl:when test=\"@label\">"); //$NON-NLS-1$
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@label\"/></a>"); //$NON-NLS-1$
		writer.println("				<br />"); //$NON-NLS-1$
		writer.println("				<div id=\"indent\">"); //$NON-NLS-1$
		writer.println("				(<xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/>)"); //$NON-NLS-1$
		writer.println("				</div>"); //$NON-NLS-1$
		writer.println("			</xsl:when>"); //$NON-NLS-1$
		writer.println("			<xsl:otherwise>"); //$NON-NLS-1$
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/></a>"); //$NON-NLS-1$
		writer.println("			</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("			</xsl:choose>"); //$NON-NLS-1$
		writer.println("			<br /><br />"); //$NON-NLS-1$
		writer.println("		</td>"); //$NON-NLS-1$
		writer.println("		<td>"); //$NON-NLS-1$
		writer.println("			<table>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@os\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Operating Systems:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@os\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@ws\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Windows Systems:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@ws\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@nl\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Languages:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@nl\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("				<xsl:if test=\"@arch\">"); //$NON-NLS-1$
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Architecture:</td>"); //$NON-NLS-1$
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@arch\"/></td>"); //$NON-NLS-1$
		writer.println("					</tr>"); //$NON-NLS-1$
		writer.println("				</xsl:if>"); //$NON-NLS-1$
		writer.println("			</table>"); //$NON-NLS-1$
		writer.println("		</td>"); //$NON-NLS-1$
		writer.println("	</tr>"); //$NON-NLS-1$
		writer.println("	</xsl:for-each>"); //$NON-NLS-1$
		writer.println("	</xsl:otherwise>"); //$NON-NLS-1$
		writer.println("	</xsl:choose>"); //$NON-NLS-1$
		writer.println("	</table>"); //$NON-NLS-1$
		writer.println("	</body>"); //$NON-NLS-1$
		writer.println("	</html>"); //$NON-NLS-1$
		writer.println("</xsl:for-each>"); //$NON-NLS-1$
		writer.println("</xsl:template>"); //$NON-NLS-1$
		writer.println("</xsl:stylesheet>"); //$NON-NLS-1$

		writer.flush();
		swrite.close();
		ByteArrayInputStream stream = new ByteArrayInputStream(swrite.toString().getBytes("UTF8")); //$NON-NLS-1$
		if (file.exists()){
			file.setContents(stream, false, false, null);
		} else {
			file.create(stream, false, null);
		}
		stream.close();
		

		} catch (Exception e){
			PDEPlugin.logException(e);
		}
	}
	private boolean createSiteProject(
		IProject project,
		IPath location,
		IProgressMonitor monitor)
		throws CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 4);

		if (!location.append(project.getName()).toFile().exists()) {
			CoreUtility.createProject(project, location, monitor);
			project.open(monitor);
			CoreUtility.addNatureToProject(
				project,
				PDE.SITE_NATURE,
				monitor);
		}
		
		createFolders(project, monitor);
		
		if (!createdProject){
			monitor.worked(2);
			monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
			// create site.xml
			IFile file = createSiteManifest(project);
			createdProject = true;
			monitor.worked(1);
			// open manifest for editing
			openSiteManifest(file);
			monitor.worked(1);
			
		} else {
			if (!project.isOpen())
				project.open(monitor);
			IFile siteFile = project.getFile("site.xml"); //$NON-NLS-1$
			if (siteFile.exists())
				openSiteManifest(siteFile);
			monitor.worked(4);
		}
		// create site.xsl, site.css, and index.html
		if (mainPage.isCreateUpdateSiteHTML()){
			createXSLFile(project);
			createCSSFile(project);
			createHTMLFile(project);
		}
		return true;
	}
	
	private void createFolders(IProject project, IProgressMonitor monitor) throws CoreException {
		String[] names = new String[]{mainPage.getWebLocation(), SiteBuild.DEFAULT_FEATURE_DIR, SiteBuild.DEFAULT_PLUGIN_DIR};
		IFolder folder;
		IPath path;
		
		for (int i =0 ; i<names.length; i++){
			if (names[i].length() ==0 || (!mainPage.isCreateUpdateSiteHTML() && i==0))
				continue;
			folder = project.getFolder(names[i]);
			path = folder.getProjectRelativePath();
			if (path.segmentCount()>0){
				for (int j = 1; j<=path.segmentCount(); j++){
					folder = project.getFolder(path.uptoSegment(j).toOSString());
					if (!folder.exists())
						createFolder(project, path.uptoSegment(j).toOSString(), monitor );
				}
			}
				
		}

		createFolder(project, PDECore.SITEBUILD_DIR, monitor);
	}
	
	private void createFolder(IProject project, String name, IProgressMonitor monitor) throws CoreException {
		IFolder plugins = project.getFolder(name);
		if (!plugins.exists())
			plugins.create(true, true, new SubProgressMonitor(monitor, 1));
		else
			monitor.worked(1);
	}

	private void openSiteManifest(IFile manifestFile) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		// Reveal the file first
		final ISelection selection = new StructuredSelection(manifestFile);
		final IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof ISetSelectionTarget) {
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					((ISetSelectionTarget) activePart).selectReveal(selection);
				}
			});
		}
		// Open the editor

		FileEditorInput input = new FileEditorInput(manifestFile);
		String id = PDEPlugin.SITE_EDITOR_ID;
		try {
			page.openEditor(input, id);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	public boolean performFinish() {
		final IProject project = mainPage.getProjectHandle();
		final IPath location = mainPage.getLocationPath();
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor){
				try {
					createSiteProject(project, location, monitor);
				} catch (CoreException e){
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
			BasicNewProjectResourceWizard.updatePerspective(config);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	public void setInitializationData(
		IConfigurationElement config,
		String property,
		Object data)
		throws CoreException {
		this.config = config;
	}
}
