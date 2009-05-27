/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.samples;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Bundle;

public class SampleOperation implements IRunnableWithProgress {
	private static final String SAMPLE_PROPERTIES = "sample.properties"; //$NON-NLS-1$

	private IConfigurationElement sample;

	private String[] projectNames;

	private IFile sampleManifest;

	private IOverwriteQuery query;

	private boolean yesToAll;

	private boolean cancel;

	private IProject[] createdProjects;

	/**
	 *  
	 */
	public SampleOperation(IConfigurationElement sample, String[] projectNames, IOverwriteQuery query) {
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
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			IWorkspaceRunnable op = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					IConfigurationElement[] projects = sample.getChildren("project"); //$NON-NLS-1$
					monitor.beginTask(PDEUIMessages.SampleOperation_creating, 4 * projects.length);
					createdProjects = new IProject[projects.length];
					try {
						for (int i = 0; i < projects.length; i++) {
							IFile file = importProject(projectNames[i], projects[i], new SubProgressMonitor(monitor, 4));
							if (file != null && sampleManifest == null)
								sampleManifest = file;
							if (file != null) {
								createdProjects[i] = file.getProject();
							}
							if (cancel)
								// if user has cancelled operation, exit.
								break;
						}
					} catch (InterruptedException e) {
						throw new OperationCanceledException();
					} catch (InvocationTargetException e) {
						throwCoreException(e);
					}
				}
			};
			PDEPlugin.getWorkspace().run(op, monitor);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} catch (OperationCanceledException e) {
			throw e;
		} finally {
			monitor.done();
		}
	}

	private void throwCoreException(InvocationTargetException e) throws CoreException {
		Throwable t = e.getCause();
		Status status = new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK, e.getMessage(), t);
		throw new CoreException(status);
	}

	private IFile importProject(String name, IConfigurationElement config, IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		String path = config.getAttribute("archive"); //$NON-NLS-1$
		if (name == null || path == null)
			return null;
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(name);
		boolean skip = false;
		if (project.exists()) {
			if (!yesToAll) {
				String returnId = query.queryOverwrite(project.getFullPath().toString());
				if (returnId.equals(IOverwriteQuery.ALL)) {
					yesToAll = true;
					skip = false;
				} else if (returnId.equals(IOverwriteQuery.YES)) {
					skip = false;
				} else if (returnId.equals(IOverwriteQuery.NO)) {
					skip = true;
				} else if (returnId.equals(IOverwriteQuery.CANCEL)) {
					skip = true;
					cancel = true;
				}
			}
			if (!skip) {
				project.delete(true, true, new SubProgressMonitor(monitor, 1));
				project = root.getProject(name);
			} else
				monitor.worked(1);
		}
		if (skip) {
			monitor.worked(3);
			IFile manifest = project.getFile(SAMPLE_PROPERTIES);
			return manifest;
		}

		project.create(new SubProgressMonitor(monitor, 1));
		project.open(new NullProgressMonitor());
		Bundle bundle = Platform.getBundle(sample.getNamespaceIdentifier());
		ZipFile zipFile = getZipFileFromPluginDir(path, bundle);
		importFilesFromZip(zipFile, project.getFullPath(), new SubProgressMonitor(monitor, 1));
		return createSampleManifest(project, config, new SubProgressMonitor(monitor, 1));
	}

	private IFile createSampleManifest(IProject project, IConfigurationElement config, IProgressMonitor monitor) throws CoreException {
		IFile file = project.getFile(SAMPLE_PROPERTIES);
		if (!file.exists()) {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Properties properties = new Properties();
				createSampleManifestContent(config.getAttribute("name"), properties); //$NON-NLS-1$
				properties.store(out, ""); //$NON-NLS-1$
				out.flush();
				String contents = out.toString();
				out.close();
				ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
				file.create(stream, true, monitor);
				stream.close();
			} catch (UnsupportedEncodingException e) {
			} catch (IOException e) {
			}
		}
		return file;
	}

	private void createSampleManifestContent(String projectName, Properties properties) {
		writeProperty(properties, "id", sample.getAttribute("id")); //$NON-NLS-1$ //$NON-NLS-2$
		writeProperty(properties, "name", sample.getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
		writeProperty(properties, "projectName", projectName); //$NON-NLS-1$
		writeProperty(properties, "launcher", sample.getAttribute("launcher")); //$NON-NLS-1$ //$NON-NLS-2$
		IConfigurationElement desc[] = sample.getChildren("description"); //$NON-NLS-1$
		if (desc.length == 1) {
			writeProperty(properties, "helpHref", desc[0] //$NON-NLS-1$
					.getAttribute("helpHref")); //$NON-NLS-1$
			writeProperty(properties, "description", desc[0].getValue()); //$NON-NLS-1$
		}
	}

	private void writeProperty(Properties properties, String name, String value) {
		if (value == null)
			return;
		properties.setProperty(name, value);
	}

	private ZipFile getZipFileFromPluginDir(String pluginRelativePath, Bundle bundle) throws CoreException {
		try {
			URL starterURL = FileLocator.resolve(bundle.getEntry(pluginRelativePath));
			return new ZipFile(FileLocator.toFileURL(starterURL).getFile());
		} catch (IOException e) {
			String message = pluginRelativePath + ": " + e.getMessage(); //$NON-NLS-1$
			Status status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, message, e);
			throw new CoreException(status);
		}
	}

	private void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		ZipFileStructureProvider structureProvider = new ZipFileStructureProvider(srcZipFile);
		ImportOperation op = new ImportOperation(destPath, structureProvider.getRoot(), structureProvider, query);
		op.run(monitor);
	}
}
