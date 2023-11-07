/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.samples;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Bundle;

public class SampleOperation implements IRunnableWithProgress {
	private static final String SAMPLE_PROPERTIES = "sample.properties"; //$NON-NLS-1$

	private final IConfigurationElement sample;

	private final String[] projectNames;

	private IFile sampleManifest;

	private final IOverwriteQuery query;

	private boolean yesToAll;

	private boolean cancel;

	private IProject[] createdProjects;

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

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			ICoreRunnable op = monitor1 -> {
				IConfigurationElement[] projects = sample.getChildren("project"); //$NON-NLS-1$
				SubMonitor subMonitor = SubMonitor.convert(monitor1, PDEUIMessages.SampleOperation_creating,
						projects.length);
				createdProjects = new IProject[projects.length];
				try {
					for (int i = 0; i < projects.length; i++) {
						IFile file = importProject(projectNames[i], projects[i],
								subMonitor.split(1));
						if (file != null && sampleManifest == null)
							sampleManifest = file;
						if (file != null) {
							createdProjects[i] = file.getProject();
						}
						if (cancel)
							// if user has cancelled operation, exit.
							break;
					}
				} catch (InterruptedException e1) {
					throw new OperationCanceledException();
				} catch (InvocationTargetException e2) {
					throwCoreException(e2);
				}
			};
			PDEPlugin.getWorkspace().run(op, monitor);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} catch (OperationCanceledException e) {
			throw e;
		}
	}

	private void throwCoreException(InvocationTargetException e) throws CoreException {
		throw new CoreException(Status.error(e.getMessage(), e.getCause()));
	}

	private IFile importProject(String name, IConfigurationElement config, IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		String path = config.getAttribute("archive"); //$NON-NLS-1$
		if (name == null || path == null)
			return null;
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(name);
		boolean skip = false;

		SubMonitor subMonitor = SubMonitor.convert(monitor, name, 5);
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
				project.delete(true, true, subMonitor.split(1));
				project = root.getProject(name);
			} else {
				subMonitor.worked(1);
			}
		}
		if (skip) {
			subMonitor.worked(4);
			IFile manifest = project.getFile(SAMPLE_PROPERTIES);
			return manifest;
		}

		project.create(subMonitor.split(1));
		project.open(subMonitor.split(1));
		Bundle bundle = Platform.getBundle(sample.getNamespaceIdentifier());
		ZipFile zipFile = getZipFileFromPluginDir(path, bundle);
		importFilesFromZip(zipFile, project.getFullPath(), subMonitor.split(1));
		return createSampleManifest(project, config, subMonitor.split(1));
	}

	private IFile createSampleManifest(IProject project, IConfigurationElement config, IProgressMonitor monitor) throws CoreException {
		IFile file = project.getFile(SAMPLE_PROPERTIES);
		if (!file.exists()) {
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				Properties properties = new Properties();
				createSampleManifestContent(config.getAttribute("name"), properties); //$NON-NLS-1$
				properties.store(out, ""); //$NON-NLS-1$
				out.flush();
				String contents = out.toString();
				try (ByteArrayInputStream stream = new ByteArrayInputStream(
						contents.getBytes(StandardCharsets.UTF_8))) {
					file.create(stream, true, monitor);
				}
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
			throw new CoreException(Status.error(pluginRelativePath + ": " + e.getMessage(), e)); //$NON-NLS-1$
		}
	}

	private void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		ZipFileStructureProvider structureProvider = new ZipFileStructureProvider(srcZipFile);
		ImportOperation op = new ImportOperation(destPath, structureProvider.getRoot(), structureProvider, query);
		op.run(monitor);
	}
}
