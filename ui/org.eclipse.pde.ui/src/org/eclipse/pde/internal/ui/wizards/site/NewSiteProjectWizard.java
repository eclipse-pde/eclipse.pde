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
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.*;

public class NewSiteProjectWizard
	extends NewWizard
	implements IExecutableExtension {
	public static final String KEY_WTITLE = "NewSiteWizard.wtitle";
	public static final String MAIN_PAGE_TITLE = "NewSiteWizard.MainPage.title";
	public static final String CREATING_PROJECT =
		"NewSiteWizard.creatingProject";
	public static final String CREATING_FOLDERS =
		"NewSiteWizard.creatingFolders";
	public static final String CREATING_MANIFEST =
		"NewSiteWizard.creatingManifest";
	public static final String MAIN_PAGE_DESC = "NewSiteWizard.MainPage.desc";
	public static final String OVERWRITE_SITE = "NewFeatureWizard.overwriteSite";

	private WizardNewProjectCreationPage mainPage;
	private SiteHTMLPage htmlPage;
	private IConfigurationElement config;
	private boolean createdProject = false;
	public static final String DEFAULT_PLUGIN_DIR = "plugins";
	public static final String DEFAULT_FEATURE_DIR = "features";

	public NewSiteProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWSITEPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	public void addPages() {
		mainPage = new WizardNewProjectCreationPage("main");
		mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
		addPage(mainPage);
		htmlPage = new SiteHTMLPage(mainPage);
		addPage(htmlPage);
	}

	private IFile createSiteManifest(
		IProject project,
		SiteData data)
		throws CoreException {
		IFile file = project.getFile("site.xml");
		if (file.exists()) return file;
		WorkspaceSiteModel model = new WorkspaceSiteModel();
		model.setFile(file);
		ISite site = model.getSite();
		String name = project.getName();
		site.setLabel(name);
		site.setType(data.type);
		site.setURL(data.url);

		// Save the model
		model.save();
		model.dispose();
		
		// Create and save build model
		WorkspaceSiteBuildModel buildModel = new WorkspaceSiteBuildModel();
		IFile buildFile = project.getFile(PDECore.SITEBUILD_FILE);
		buildModel.setFile(buildFile);
		ISiteBuild siteBuild = buildModel.getSiteBuild();
	 	siteBuild.setAutobuild(false);
	 	siteBuild.setPluginLocation(new Path(DEFAULT_PLUGIN_DIR));
	 	siteBuild.setFeatureLocation(new Path(DEFAULT_FEATURE_DIR));
	 	siteBuild.setShowConsole(true);
	 	buildModel.save();
		buildModel.dispose();
		
		// Set the default editor
		IDE.setDefaultEditor(file, PDEPlugin.SITE_EDITOR_ID);
		return file;
	}
	
	private void createHTMLFile(IProject project){
		try {
		IFile file = project.getFile("index.html");
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<title>"+project.getName()+"</title>");
		writer.println("<style>@import url(\""+htmlPage.getWebLocation()+"/site.css\");</style>");
		writer.println("<script type=\"text/javascript\">");
		writer.println("	var returnval = 0;");
		writer.println("	var stylesheet, xmlFile, cache, doc;");
		writer.println("	function init(){");
		writer.println("		// NSCP 7.1+ / Mozilla 1.4.1+");
		writer.println("		// Use the standard DOM Level 2 technique, if it is supported");
		writer.println("		if (document.implementation && document.implementation.createDocument) {");
		writer.println("			xmlFile = document.implementation.createDocument(\"\", \"\", null);");
		writer.println("			stylesheet = document.implementation.createDocument(\"\", \"\", null);");
		writer.println("			xmlFile.load(\"site.xml\");");
		writer.println("			stylesheet.load(\""+htmlPage.getWebLocation()+"/site.xsl\");");
		writer.println("			xmlFile.addEventListener(\"load\", transform, false);");
		writer.println("			stylesheet.addEventListener(\"load\", transform, false);");
		writer.println("		}");
		writer.println("		//IE 6.0+ solution");
		writer.println("		else if (window.ActiveXObject) {");
		writer.println("			xmlFile = new ActiveXObject(\"msxml2.DOMDocument.3.0\");");
		writer.println("			xmlFile.async = false;");
		writer.println("			xmlFile.load(\"site.xml\");");
		writer.println("			stylesheet = new ActiveXObject(\"msxml2.FreeThreadedDOMDocument.3.0\");");
		writer.println("			stylesheet.async = false;");
		writer.println("			stylesheet.load(\""+htmlPage.getWebLocation()+"/site.xsl\");");
		writer.println("			cache = new ActiveXObject(\"msxml2.XSLTemplate.3.0\");");
		writer.println("			cache.stylesheet = stylesheet;");
		writer.println("			transformData();");
		writer.println("		}");
		writer.println("	}");
		writer.println("	// separate transformation function for IE 6.0+");
		writer.println("	function transformData(){");
		writer.println("		var processor = cache.createProcessor();");
		writer.println("		processor.input = xmlFile;");
		writer.println("		processor.transform();");
		writer.println("		data.innerHTML = processor.output;");
		writer.println("	}");
		writer.println("	// separate transformation function for NSCP 7.1+ and Mozilla 1.4.1+ ");
		writer.println("	function transform(){");
		writer.println("		returnval+=1;");
		writer.println("		if (returnval==2){");
		writer.println("			var processor = new XSLTProcessor();");
		writer.println("			processor.importStylesheet(stylesheet); ");
		writer.println("			doc = processor.transformToDocument(xmlFile);");
		writer.println("			document.getElementById(\"data\").innerHTML = doc.documentElement.innerHTML;");
		writer.println("		}");
		writer.println("	}");
		writer.println("</script>");
		writer.println("</head>");
		writer.println("<body onload=\"init();\">");
		writer.println("<!--[insert static HTML here]-->");
		writer.println("<div id=\"data\"><!-- this is where the transformed data goes --></div>");
		writer.println("</body>");
		writer.println("</html>");

		writer.flush();
		swrite.close();
		ByteArrayInputStream stream = new ByteArrayInputStream(swrite.toString().getBytes("UTF8"));
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
		IFile file = project.getFile(htmlPage.getWebLocation() + "/site.css");
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<STYLE type=\"text/css\">");
		writer.println("td.spacer {padding-bottom: 10px; padding-top: 10px;}");
		writer.println(".title { font-family: sans-serif; color: #99AACC;}");
		writer.println(".bodyText { font-family: sans-serif; font-size: 9pt; color:#000000;  }");
		writer.println(".sub-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white;}");
		writer.println(".log-text {font-family: sans-serif; font-style: normal; font-weight: lighter; font-size: 8pt; color:black;}");
		writer.println(".big-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white; border-top:10px solid white;}");
		writer.println(".light-row {background:#FFFFFF}");
		writer.println(".dark-row {background:#EEEEFF}");
		writer.println(".header {background:#99AADD}");
		writer.println("#indent {word-wrap : break-word;width :300px;text-indent:10px;}");
		writer.println("</STYLE>");

		writer.flush();
		swrite.close();
		ByteArrayInputStream stream = new ByteArrayInputStream(swrite.toString().getBytes("UTF8"));
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
		IFile file = project.getFile(htmlPage.getWebLocation() + "/site.xsl");
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<xsl:stylesheet version = '1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns:msxsl=\"urn:schemas-microsoft-com:xslt\">");
		writer.println("<xsl:output method=\"html\" encoding=\"ISO-8859-1\"/>");
		writer.println("<xsl:key name=\"cat\" match=\"category\" use=\"@name\"/>");
		writer.println("<xsl:template match=\"/\">");
		writer.println("<xsl:for-each select=\"site\">");
		writer.println("	<html>");
		writer.println("	<head>");
		writer.println("	<title>"+project.getName()+"</title>");
		writer.println("	<style>@import url(\"" + htmlPage.getWebLocation() + "/site.css\");</style>");
		writer.println("	</head>");
		writer.println("	<body>");
		writer.println("	<h1 class=\"title\">" + project.getName() +"</h1>");
		writer.println("	<p class=\"bodyText\"><xsl:value-of select=\"description\"/></p>");
		writer.println("	<table width=\"100%\" border=\"0\" cellspacing=\"1\" cellpadding=\"2\">");
		writer.println("	<xsl:for-each select=\"category-def\">");
		writer.println("		<xsl:sort select=\"@label\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("		<xsl:sort select=\"@name\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("	<xsl:if test=\"count(key('cat',@name)) != 0\">");
		writer.println("			<tr class=\"header\">");
		writer.println("				<td class=\"sub-header\" width=\"30%\">");
		writer.println("					<xsl:value-of select=\"@name\"/>");
		writer.println("				</td>");
		writer.println("				<td class=\"sub-header\" width=\"70%\">");
		writer.println("					<xsl:value-of select=\"@label\"/>");
		writer.println("				</td>");
		writer.println("			</tr>");
		writer.println("			<xsl:for-each select=\"key('cat',@name)\">");
		writer.println("			<xsl:sort select=\"ancestor::feature//@version\" order=\"ascending\"/>");
		writer.println("			<xsl:sort select=\"ancestor::feature//@id\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("			<tr>");
		writer.println("				<xsl:choose>");
		writer.println("				<xsl:when test=\"(position() mod 2 = 1)\">");
		writer.println("					<xsl:attribute name=\"class\">dark-row</xsl:attribute>");
		writer.println("				</xsl:when>");
		writer.println("				<xsl:otherwise>");
		writer.println("					<xsl:attribute name=\"class\">light-row</xsl:attribute>");
		writer.println("				</xsl:otherwise>");
		writer.println("				</xsl:choose>");
		writer.println("				<td class=\"log-text\" id=\"indent\">");
		writer.println("						<xsl:choose>");
		writer.println("						<xsl:when test=\"ancestor::feature//@label\">");
		writer.println("							<a href=\"{ancestor::feature//@url}\"><xsl:value-of select=\"ancestor::feature//@label\"/></a>");
		writer.println("							<br/>");
		writer.println("							<div id=\"indent\">");
		writer.println("							(<xsl:value-of select=\"ancestor::feature//@id\"/> - <xsl:value-of select=\"ancestor::feature//@version\"/>)");
		writer.println("							</div>");
		writer.println("						</xsl:when>");
		writer.println("						<xsl:otherwise>");
		writer.println("						<a href=\"{ancestor::feature//@url}\"><xsl:value-of select=\"ancestor::feature//@id\"/> - <xsl:value-of select=\"ancestor::feature//@version\"/></a>");
		writer.println("						</xsl:otherwise>");
		writer.println("						</xsl:choose>");
		writer.println("						<br />");
		writer.println("				</td>");
		writer.println("				<td>");
		writer.println("					<table>");
		writer.println("						<xsl:if test=\"ancestor::feature//@os\">");
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Operating Systems:</td>");
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@os\"/></td>");
		writer.println("							</tr>");
		writer.println("						</xsl:if>");
		writer.println("						<xsl:if test=\"ancestor::feature//@ws\">");
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Windows Systems:</td>");
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@ws\"/></td>");
		writer.println("							</tr>");
		writer.println("						</xsl:if>");
		writer.println("						<xsl:if test=\"ancestor::feature//@nl\">");
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Languages:</td>");
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@nl\"/></td>");
		writer.println("							</tr>");
		writer.println("						</xsl:if>");
		writer.println("						<xsl:if test=\"ancestor::feature//@arch\">");
		writer.println("							<tr><td class=\"log-text\" id=\"indent\">Architecture:</td>");
		writer.println("							<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"ancestor::feature//@arch\"/></td>");
		writer.println("							</tr>");
		writer.println("						</xsl:if>");
		writer.println("					</table>");
		writer.println("				</td>");
		writer.println("			</tr>");
		writer.println("			</xsl:for-each>");
		writer.println("			<tr><td class=\"spacer\"><br/></td><td class=\"spacer\"><br/></td></tr>");
		writer.println("		</xsl:if>");
		writer.println("	</xsl:for-each>");
		writer.println("	<xsl:if test=\"count(feature)  &gt; count(feature/category)\">");
		writer.println("	<tr class=\"header\">");
		writer.println("		<td class=\"sub-header\" colspan=\"2\">");
		writer.println("		Uncategorized");
		writer.println("		</td>");
		writer.println("	</tr>");
		writer.println("	</xsl:if>");
		writer.println("	<xsl:choose>");
		writer.println("	<xsl:when test=\"function-available('msxsl:node-set')\">");
		writer.println("	   <xsl:variable name=\"rtf-nodes\">");
		writer.println("		<xsl:for-each select=\"feature[not(category)]\">");
		writer.println("			<xsl:sort select=\"@id\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("			<xsl:sort select=\"@version\" order=\"ascending\" />");
		writer.println("			<xsl:value-of select=\".\"/>");
		writer.println("			<xsl:copy-of select=\".\" />");
		writer.println("		</xsl:for-each>");
		writer.println("	   </xsl:variable>");
		writer.println("	   <xsl:variable name=\"myNodeSet\" select=\"msxsl:node-set($rtf-nodes)/*\"/>");
		writer.println("	<xsl:for-each select=\"$myNodeSet\">");
		writer.println("	<tr>");
		writer.println("		<xsl:choose>");
		writer.println("		<xsl:when test=\"position() mod 2 = 1\">");
		writer.println("		<xsl:attribute name=\"class\">dark-row</xsl:attribute>");
		writer.println("		</xsl:when>");
		writer.println("		<xsl:otherwise>");
		writer.println("		<xsl:attribute name=\"class\">light-row</xsl:attribute>");
		writer.println("		</xsl:otherwise>");
		writer.println("		</xsl:choose>");
		writer.println("		<td class=\"log-text\" id=\"indent\">");
		writer.println("			<xsl:choose>");
		writer.println("			<xsl:when test=\"@label\">");
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@label\"/></a>");
		writer.println("				<br />");
		writer.println("				<div id=\"indent\">");
		writer.println("				(<xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/>)");
		writer.println("				</div>");
		writer.println("			</xsl:when>");
		writer.println("			<xsl:otherwise>");
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/></a>");
		writer.println("			</xsl:otherwise>");
		writer.println("			</xsl:choose>");
		writer.println("			<br /><br />");
		writer.println("		</td>");
		writer.println("		<td>");
		writer.println("			<table>");
		writer.println("				<xsl:if test=\"@os\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Operating Systems:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@os\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("				<xsl:if test=\"@ws\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Windows Systems:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@ws\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("				<xsl:if test=\"@nl\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Languages:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@nl\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("				<xsl:if test=\"@arch\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Architecture:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@arch\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("			</table>");
		writer.println("		</td>");
		writer.println("	</tr>");
		writer.println("	</xsl:for-each>");
		writer.println("	</xsl:when>");
		writer.println("	<xsl:otherwise>");
		writer.println("	<xsl:for-each select=\"feature[not(category)]\">");
		writer.println("	<xsl:sort select=\"@id\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("	<xsl:sort select=\"@version\" order=\"ascending\" />");
		writer.println("	<tr>");
		writer.println("		<xsl:choose>");
		writer.println("		<xsl:when test=\"count(preceding-sibling::feature[not(category)]) mod 2 = 1\">");
		writer.println("		<xsl:attribute name=\"class\">dark-row</xsl:attribute>");
		writer.println("		</xsl:when>");
		writer.println("		<xsl:otherwise>");
		writer.println("		<xsl:attribute name=\"class\">light-row</xsl:attribute>");
		writer.println("		</xsl:otherwise>");
		writer.println("		</xsl:choose>");
		writer.println("		<td class=\"log-text\" id=\"indent\">");
		writer.println("			<xsl:choose>");
		writer.println("			<xsl:when test=\"@label\">");
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@label\"/></a>");
		writer.println("				<br />");
		writer.println("				<div id=\"indent\">");
		writer.println("				(<xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/>)");
		writer.println("				</div>");
		writer.println("			</xsl:when>");
		writer.println("			<xsl:otherwise>");
		writer.println("				<a href=\"{@url}\"><xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/></a>");
		writer.println("			</xsl:otherwise>");
		writer.println("			</xsl:choose>");
		writer.println("			<br /><br />");
		writer.println("		</td>");
		writer.println("		<td>");
		writer.println("			<table>");
		writer.println("				<xsl:if test=\"@os\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Operating Systems:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@os\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("				<xsl:if test=\"@ws\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Windows Systems:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@ws\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("				<xsl:if test=\"@nl\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Languages:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@nl\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("				<xsl:if test=\"@arch\">");
		writer.println("					<tr><td class=\"log-text\" id=\"indent\">Architecture:</td>");
		writer.println("					<td class=\"log-text\" id=\"indent\"><xsl:value-of select=\"@arch\"/></td>");
		writer.println("					</tr>");
		writer.println("				</xsl:if>");
		writer.println("			</table>");
		writer.println("		</td>");
		writer.println("	</tr>");
		writer.println("	</xsl:for-each>");
		writer.println("	</xsl:otherwise>");
		writer.println("	</xsl:choose>");
		writer.println("	</table>");
		writer.println("	</body>");
		writer.println("	</html>");
		writer.println("</xsl:for-each>");
		writer.println("</xsl:template>");
		writer.println("</xsl:stylesheet>");

		writer.flush();
		swrite.close();
		ByteArrayInputStream stream = new ByteArrayInputStream(swrite.toString().getBytes("UTF8"));
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
		SiteData data,
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
			IFile file = createSiteManifest(project, data);
			createdProject = true;
			monitor.worked(1);
			// open manifest for editing
			openSiteManifest(file);
			monitor.worked(1);
			
		} else {
			if (!project.isOpen())
				project.open(monitor);
			IFile siteFile = project.getFile("site.xml");
			if (siteFile.exists())
				openSiteManifest(siteFile);
			monitor.worked(4);
		}
		// create site.xsl, site.css, and index.html
		if (htmlPage.isCreateUpdateSiteHTML()){
			createXSLFile(project);
			createCSSFile(project);
			createHTMLFile(project);
		}
		return true;
	}
	
	private void createFolders(IProject project, IProgressMonitor monitor) throws CoreException {
		String[] names = new String[]{htmlPage.getWebLocation(), DEFAULT_FEATURE_DIR, DEFAULT_PLUGIN_DIR};
		IFolder folder;
		IPath path;
		
		for (int i =0 ; i<names.length; i++){
			if (names[i].length() ==0 || (!htmlPage.isCreateUpdateSiteHTML() && i==0))
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
		final SiteData data = new SiteData();
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor){
				try {
					createSiteProject(project, location, data, monitor);
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
