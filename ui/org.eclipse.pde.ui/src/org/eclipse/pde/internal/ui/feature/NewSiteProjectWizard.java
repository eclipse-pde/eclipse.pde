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
package org.eclipse.pde.internal.ui.feature;

import java.io.*;
import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
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
	 	siteBuild.setPluginLocation(new Path("plugins"));
	 	siteBuild.setFeatureLocation(new Path("features"));
	 	siteBuild.setShowConsole(true);
	 	buildModel.save();
		buildModel.dispose();
		
		// Set the default editor
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.SITE_EDITOR_ID);
		return file;
	}
	
	private void createHTMLFile(IProject project){
		try {
		IFile file = project.getFile("index.html");
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<script type=\"text/javascript\">");
		writer.println("	var returnval = 0;");
		writer.println("	function transform(){");
		writer.println("		returnval+=1;");
		writer.println("		if (returnval==2){");
		writer.println("			var processor = new XSLTProcessor();");
		writer.println("			processor.importStylesheet(transformer); ");
		writer.println("			var doc = processor.transformToDocument(data);");
		writer.println("			document.write(doc.documentElement.innerHTML);");
		writer.println("		}");
		writer.println("	}");
		writer.println("	// NSCP 7.1+ / Mozilla 1.2+");
		writer.println("	// Use the standard DOM Level 2 technique, if it is supported");
		writer.println("	if (document.implementation && document.implementation.createDocument) {");
		writer.println("		var data = document.implementation.createDocument(\"\", \"\", null);");
		writer.println("		var transformer = document.implementation.createDocument(\"\", \"\", null);");
		writer.println("		data.load(\"site.xml\");");
		writer.println("		transformer.load(\"site.xsl\");");
		writer.println("		data.addEventListener(\"load\", transform, false);");
		writer.println("		transformer.addEventListener(\"load\", transform, false);");
		writer.println("	}");
		writer.println("	//IE 6.0+ solution");
		writer.println("	else if (window.ActiveXObject) {");
		writer.println("		var xml = new ActiveXObject(\"MSXML2.DOMDocument.3.0\")");
		writer.println("		xml.async = false");
		writer.println("		xml.load(\"site.xml\")");
		writer.println("		var xsl = new ActiveXObject(\"MSXML2.DOMDocument.3.0\")");
		writer.println("		xsl.async = false");
		writer.println("		xsl.load(\"site.xsl\")");
		writer.println("		// Transform");
		writer.println("		document.write(xml.transformNode(xsl))");
		writer.println("	}");
		writer.println("</script>");
		writer.println("</head>");
		writer.println("<body>");
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
		IFile file = project.getFile("site.css");
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<STYLE type=\"text/css\">");
		writer.println(".css{}");
		writer.println(".title { font-family: sans-serif; color: #99AACC;}");
		writer.println(".bodyText { font-family: sans-serif; font-size: 9pt; color:#000000;  }");
		writer.println(".sub-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white;}");
		writer.println(".log-text { font-family: sans-serif; font-style: normal; font-weight: lighter; font-size: 8pt; color:black;}");
		writer.println(".big-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white; border-top:10px solid white;}");
		writer.println(".light-row {background:#FFFFFF}");
		writer.println(".dark-row {background:#EEEEFF}");
		writer.println(".header {background:#99AADD}");
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
		IFile file = project.getFile("site.xsl");
		StringWriter swrite = new StringWriter();
		PrintWriter writer = new PrintWriter(swrite);
		writer.println("<xsl:stylesheet version = '1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>");
		writer.println("<xsl:output method=\"html\" encoding=\"ISO-8859-1\"/>");
		writer.println("<xsl:key name=\"cat\" match=\"category\" use=\"@name\"/>");
		writer.println("<xsl:template match=\"/\">");
		writer.println("<xsl:for-each select=\"site\">");
		writer.println("	<html>");
		writer.println("	<head>");
		writer.println("    <title>" + project.getName() + "</title>");
		writer.println("	<style>@import url(\"site.css\");</style>");
		writer.println("	</head>");
		writer.println("	<body>");
		writer.println("	<h1 class=\"title\">" + project.getName() + "</h1>");
		writer.println("	<p class=\"bodyText\"><xsl:value-of select=\"description\"/></p>");
		writer.println("	<table width=\"100%\" border=\"0\" cellspacing=\"1\" cellpadding=\"2\">");
		writer.println("	<tr class=\"header\">");
		writer.println("		<td colspan=\"3\" class=\"big-header\">");
		writer.println("		Features Index:");
		writer.println("		</td>");
		writer.println("	</tr>");
		writer.println("	<tr class=\"header\">");
		writer.println("		<td class=\"sub-header\">");
		writer.println("		Category");
		writer.println("		</td>");
		writer.println("		<td class=\"sub-header\">");
		writer.println("		Description");
		writer.println("		</td>");
		writer.println("		<td class=\"sub-header\">");
		writer.println("		Features");
		writer.println("		</td>");
		writer.println("	</tr>");
		writer.println("	<xsl:for-each select=\"category-def\">");
		writer.println("	<tr>");
		writer.println("	<xsl:choose>");
		writer.println("	<xsl:when test=\"(position() mod 2 = 1)\">");
		writer.println("	<xsl:attribute name=\"class\">dark-row</xsl:attribute>");
		writer.println("	</xsl:when>");
		writer.println("	<xsl:otherwise>");
		writer.println("	<xsl:attribute name=\"class\">light-row</xsl:attribute>");
		writer.println("	</xsl:otherwise>");
		writer.println("	</xsl:choose>");
		writer.println("		<td class=\"log-text\">");
		writer.println("			<xsl:value-of select=\"@name\"/>");
		writer.println("		</td>");
		writer.println("		<td class=\"log-text\">");
		writer.println("			<xsl:value-of select=\"description\"/>");
		writer.println("		</td>");
		writer.println("		<td class=\"log-text\">");
		writer.println("			<xsl:for-each select=\"key('cat',@name)\">");
		writer.println("				<xsl:sort select=\"ancestor::feature//@version\" order=\"ascending\"/>");
		writer.println("				<xsl:sort select=\"ancestor::feature//@id\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("				<a href=\"{ancestor::feature//@url}\"><xsl:value-of select=\"ancestor::feature//@id\"/> - <xsl:value-of select=\"ancestor::feature//@version\"/></a>");
		writer.println("				<xsl:choose>");
		writer.println("				<xsl:when test=\"ancestor::feature//@label\">");
		writer.println("					(<xsl:value-of select=\"ancestor::feature//@label\"/>)");
		writer.println("				</xsl:when>");
		writer.println("				</xsl:choose>");
		writer.println("				<br />");
		writer.println("			</xsl:for-each>");
		writer.println("		</td>");
		writer.println("	</tr>");
		writer.println("	</xsl:for-each>");
		writer.println("	<tr>");
		writer.println("	<xsl:choose>");
		writer.println("	<xsl:when test=\"(count(ancestor::category-def) mod 2 = 1)\">");
		writer.println("	<xsl:attribute name=\"class\">dark-row</xsl:attribute>");
		writer.println("	</xsl:when>");
		writer.println("	<xsl:otherwise>");
		writer.println("	<xsl:attribute name=\"class\">light-row</xsl:attribute>");
		writer.println("	</xsl:otherwise>");
		writer.println("	</xsl:choose>");
		writer.println("	<td class=\"log-text\">N/A</td>");
		writer.println("	<td class=\"log-text\">N/A</td>");
		writer.println("	<td class=\"log-text\">");
		writer.println("		<xsl:for-each select=\"feature\">");
		writer.println("		<xsl:sort select=\"@id\" order=\"ascending\" case-order=\"upper-first\"/>");
		writer.println("		<xsl:sort select=\"@version\" order=\"ascending\" />");
		writer.println("		<xsl:if test=\"not(category)\">");
		writer.println("			<a href=\"{@url}\"><xsl:value-of select=\"@id\"/> - <xsl:value-of select=\"@version\"/></a>");
		writer.println("			<xsl:if test=\"@label\">");
		writer.println("				(<xsl:value-of select=\"@label\"/>)");
		writer.println("			</xsl:if>");
		writer.println("			<br/>");
		writer.println("		</xsl:if>");
		writer.println("		</xsl:for-each>");
		writer.println("	</td>");
		writer.println("	</tr>");
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
	private void createSiteProject(
		IProject project,
		IPath location,
		SiteData data,
		boolean createSiteHTML,
		IProgressMonitor monitor)
		throws CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 4);

		boolean overwrite = true;
		if (location.append(project.getName()).toFile().exists()) {
			overwrite =
				MessageDialog.openQuestion(
					PDEPlugin.getActiveWorkbenchShell(),
					getWindowTitle(),
					PDEPlugin.getResourceString(OVERWRITE_SITE));
		}
		if (overwrite) {
			CoreUtility.createProject(project, location, monitor);
			project.open(monitor);
			CoreUtility.addNatureToProject(
				project,
				PDE.SITE_NATURE,
				monitor);
			createFolders(project, monitor);
			monitor.worked(2);
			monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
			// create site.xml
			IFile file = createSiteManifest(project, data);
			monitor.worked(1);
			// open manifest for editing
			openSiteManifest(file);
			monitor.worked(1);
		} else {
			project.create(monitor);
			project.open(monitor);
			IFile siteFile = project.getFile("site.xml");
			if (siteFile.exists())
				openSiteManifest(siteFile);
			monitor.worked(4);
		}
		// create site.xsl, site.css, and index.html
		if (createSiteHTML){
			createXSLFile(project);
			createCSSFile(project);
			createHTMLFile(project);
		}
	}
	
	private void createFolders(IProject project, IProgressMonitor monitor) throws CoreException {
		createFolder(project, "features", monitor);
		createFolder(project, "plugins", monitor);
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
		final boolean createHTMLSite = htmlPage.isCreateUpdateSiteHTML();
		
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					createSiteProject(project, location, data, createHTMLSite, monitor);
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
