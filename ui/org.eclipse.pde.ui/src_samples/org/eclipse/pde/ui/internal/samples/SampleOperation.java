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
package org.eclipse.pde.ui.internal.samples;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class SampleOperation implements IRunnableWithProgress {
	private static final String SAMPLE_PROPERTIES = "sample.properties";
	private IConfigurationElement sample;
	private String [] projectNames;
	private IFile sampleManifest;
	private IOverwriteQuery query;
	private boolean noToAll;
	private IProject [] createdProjects;
	/**
	 *  
	 */
	public SampleOperation(IConfigurationElement sample, String [] projectNames, IOverwriteQuery query) {
		this.sample = sample;
		this.query = query;
		this.projectNames = projectNames;
	}
	public IFile getSampleManifest() {
		return sampleManifest;
	}
	public IProject[] getCreatedProjects() {
		return createdProjects;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			IConfigurationElement[] projects = sample.getChildren("project");
			monitor.beginTask("Creating projects...", 4 * projects.length);
			createdProjects = new IProject[projects.length];			
			for (int i = 0; i < projects.length; i++) {
				IFile file = importProject(projectNames[i], projects[i], new SubProgressMonitor(
						monitor, 4));
				if (file != null && sampleManifest == null)
					sampleManifest = file;
				if (file!=null) {
					createdProjects[i] = file.getProject();
				}
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}
	private IFile importProject(String name, IConfigurationElement config,
			IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		String path = config.getAttribute("archive");
		if (name == null || path == null)
			return null;
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(name);
		boolean skip = false;
		if (project.exists()) {
			if (noToAll)
				skip = true;
			else {
				String returnId = query.queryOverwrite(project.getFullPath()
						.toString());
				if (returnId.equals(IOverwriteQuery.NO_ALL)) {
					noToAll = true;
					skip = true;
				} else if (returnId.equals(IOverwriteQuery.NO)) {
					skip = true;
				}
			}
			if (!skip) {
				project.delete(true, true, new SubProgressMonitor(monitor, 1));
				project = root.getProject(name);
			}
			else
				monitor.worked(1);
		}
		if (skip) {
			monitor.worked(3);
			IFile manifest = project.getFile(SAMPLE_PROPERTIES);
			return manifest;
		}
		project.create(new SubProgressMonitor(monitor, 1));
		project.open(new NullProgressMonitor());
		ZipFile zipFile = getZipFileFromPluginDir(path, sample
				.getDeclaringExtension().getDeclaringPluginDescriptor());
		importFilesFromZip(zipFile, project.getFullPath(),
				new SubProgressMonitor(monitor, 1));
		return createSampleManifest(project, config, new SubProgressMonitor(
				monitor, 1));
	}
	private IFile createSampleManifest(IProject project,
			IConfigurationElement config, IProgressMonitor monitor)
			throws CoreException {
		IFile file = project.getFile(SAMPLE_PROPERTIES);
		if (!file.exists()) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Properties properties = new Properties();
				createSampleManifestContent(config.getAttribute("name"), properties);
				properties.store(out, "");
				out.flush();
				String contents = out.toString();
				out.close();
				ByteArrayInputStream stream = new ByteArrayInputStream(contents
						.getBytes("UTF8"));
				file.create(stream, true, monitor);
				stream.close();
			} catch (UnsupportedEncodingException e) {
			} catch (IOException e) {
			}
		}
		return file;
	}
	private void createSampleManifestContent(String projectName, Properties properties) {
		writeProperty(properties, "id", sample.getAttribute("id"));
		writeProperty(properties, "name", sample.getAttribute("name"));
		writeProperty(properties, "projectName", projectName);
		writeProperty(properties, "launcher", sample.getAttribute("launcher"));
		IConfigurationElement desc[] = sample.getChildren("description");
		if (desc.length == 1) {
			writeProperty(properties, "helpHref", desc[0]
					.getAttribute("helpHref"));
			writeProperty(properties, "description", desc[0].getValue());
		}
	}
	private void writeProperty(Properties properties, String name, String value) {
		if (value == null)
			return;
		properties.setProperty(name, value);
	}
	private ZipFile getZipFileFromPluginDir(String pluginRelativePath,
			IPluginDescriptor pluginDescriptor) throws CoreException {
		try {
			URL starterURL = new URL(pluginDescriptor.getInstallURL(),
					pluginRelativePath);
			return new ZipFile(Platform.asLocalURL(starterURL).getFile());
		} catch (IOException e) {
			String message = pluginRelativePath + ": " + e.getMessage(); //$NON-NLS-1$
			Status status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
					IStatus.ERROR, message, e);
			throw new CoreException(status);
		}
	}
	private void importFilesFromZip(ZipFile srcZipFile, IPath destPath,
			IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		ZipFileStructureProvider structureProvider = new ZipFileStructureProvider(
				srcZipFile);
		ImportOperation op = new ImportOperation(destPath, structureProvider
				.getRoot(), structureProvider, query);
		op.run(monitor);
	}
}