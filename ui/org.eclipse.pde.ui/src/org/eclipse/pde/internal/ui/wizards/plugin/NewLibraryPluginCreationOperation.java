/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Constants;

public class NewLibraryPluginCreationOperation extends
		NewProjectCreationOperation {

	private LibraryPluginFieldData fData;

	public NewLibraryPluginCreationOperation(LibraryPluginFieldData data,
			IProjectProvider provider, IPluginContentWizard contentWizard) {
		super(data, provider, contentWizard);
		fData = data;
	}

	private void addJar(File jarFile, IProject project, IProgressMonitor monitor)
			throws CoreException {
		String jarName = jarFile.getName();
		IFile file = project.getFile(jarName);
		monitor.subTask(NLS.bind(
				PDEUIMessages.NewProjectCreationOperation_copyingJar, jarName)); 
		InputStream in = null;
		try {
			in = new FileInputStream(jarFile);
			file.create(in, true, monitor);
		} catch (FileNotFoundException fnfe) {
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

	private void adjustExportRoot(IProgressMonitor monitor, IProject project)
			throws CoreException {
		IResource[] resources = project.members(false);
		for (int j = 0; j < resources.length; j++) {
			if (resources[j] instanceof IFile) {
				if (".project".equals(resources[j].getName()) //$NON-NLS-1$
						|| ".classpath".equals(resources[j] //$NON-NLS-1$
								.getName()) || "plugin.xml".equals(resources[j] //$NON-NLS-1$
								.getName())
						|| "build.properties".equals(resources[j] //$NON-NLS-1$
								.getName())) {
					continue;
				}
				// resource at the root, export root
				return;
			}
		}
		removeExportRoot(project.getFile("META-INF/MANIFEST.MF"), monitor); //$NON-NLS-1$
	}

	protected void adjustManifests(IProgressMonitor monitor, IProject project)
			throws CoreException {
		super.adjustManifests(monitor, project);
		if (fData.hasBundleStructure() && fData.isUnzipLibraries()) {
			adjustExportRoot(monitor, project);
		}
	}

	protected void createContents(IProgressMonitor monitor, IProject project)
			throws CoreException, JavaModelException,
			InvocationTargetException, InterruptedException {
		// copy jars
		String[] paths = fData.getLibraryPaths();
		for (int i = paths.length - 1; i >= 0; i--) {
			File jarFile = new File(paths[i]);
			if (fData.isUnzipLibraries()) {
				importJar(jarFile, project, monitor);
			} else {
				addJar(jarFile, project, monitor);
			}
			monitor.worked(1);
		}
		if (!fData.hasBundleStructure()) {
			// delete manifest.mf imported from libraries
			IFile importedManifest = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
			if (importedManifest.exists()) {
				importedManifest.delete(true, false, monitor);
				if (!fData.hasBundleStructure()) {
					IFolder meta_inf = project.getFolder("META-INF"); //$NON-NLS-1$
					if (meta_inf.members().length == 0) {
						meta_inf.delete(true, false, monitor);
					}
				}
			}
		}
	}

	protected void fillBinIncludes(IProject project, IBuildEntry binEntry)
			throws CoreException {
		if (fData.hasBundleStructure())
			binEntry.addToken("META-INF/"); //$NON-NLS-1$
		else
			binEntry.addToken("plugin.xml"); //$NON-NLS-1$

		if (fData.isUnzipLibraries()) {
			IResource[] resources = project.members(false);
			for (int j = 0; j < resources.length; j++) {
				if (resources[j] instanceof IFolder) {
					if (!binEntry.contains(resources[j].getName() + "/")) //$NON-NLS-1$
						binEntry.addToken(resources[j].getName() + "/"); //$NON-NLS-1$
				} else {
					if (".project".equals(resources[j].getName()) //$NON-NLS-1$
							|| ".classpath".equals(resources[j] //$NON-NLS-1$
									.getName())
							|| "build.properties".equals(resources[j] //$NON-NLS-1$
									.getName())) {
						continue;
					}
					if (!binEntry.contains(resources[j].getName()))
						binEntry.addToken(resources[j].getName());
				}
			}
		} else {
			String[] libraryPaths = fData.getLibraryPaths();
			for (int j = 0; j < libraryPaths.length; j++) {
				File jarFile = new File(libraryPaths[j]);
				String name = jarFile.getName();
				if (!binEntry.contains(name))
					binEntry.addToken(name);
			}
		}
	}

	protected IClasspathEntry[] getInternalClassPathEntries(IProject project,
			IFieldData data) {
		String[] libraryPaths;
		if (fData.isUnzipLibraries()) {
			libraryPaths = new String[] { "" }; //$NON-NLS-1$
		} else {
			libraryPaths = fData.getLibraryPaths();
		}
		IClasspathEntry[] entries = new IClasspathEntry[libraryPaths.length];
		for (int j = 0; j < libraryPaths.length; j++) {
			File jarFile = new File(libraryPaths[j]);
			String jarName = jarFile.getName();
			IPath path = project.getFullPath().append(jarName);
			entries[j] = JavaCore.newLibraryEntry(path, null, null, true);
		}
		return entries;
	}

	protected int getNumberOfWorkUnits() {
		int numUnits = super.getNumberOfWorkUnits();
		numUnits += fData.getLibraryPaths().length;
		return numUnits;
	}

	private void importJar(File jar, IResource destination,
			IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		ZipFile input = null;
		try {
			try {
				input = new ZipFile(jar);
				ZipFileStructureProvider provider = new ZipFileStructureProvider(
						input);
				ImportOperation op = new ImportOperation(destination
						.getFullPath(), provider.getRoot(), provider,
						new IOverwriteQuery() {
							public String queryOverwrite(String pathString) {
								return IOverwriteQuery.ALL;
							}
						});
				op.run(monitor);
			} finally {
				if (input != null)
					input.close();
			}
		} catch (IOException e) {
			throw new CoreException(
					new Status(
							IStatus.ERROR,
							PDEPlugin.PLUGIN_ID,
							IStatus.OK,
							NLS
									.bind(
											PDEUIMessages.NewProjectCreationOperation_errorImportingJar,
											jar), e));
		}
	}

	private void removeExportRoot(IFile file, IProgressMonitor monitor)
			throws CoreException {
		if (!file.exists())
			return;
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file
					.getFullPath());

			IDocument document = buffer.getDocument();
			BundleModel model = new BundleModel(document, false);
			model.load();
			TextEdit edit = removeRootExportPackage(model);
			if (edit != null) {
				try {
					edit.apply(document);
				} catch (BadLocationException e) {
					PDEPlugin.logException(e);
				}
				buffer.commit(monitor, true);
			}
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}

	}

	private TextEdit removeRootExportPackage(BundleModel model) {
		Bundle bundle = (Bundle) model.getBundle();
		ExportPackageHeader header = (ExportPackageHeader) bundle
				.getManifestHeader(Constants.EXPORT_PACKAGE);
		ExportPackageObject[] packages = header.getPackages();
		for (int i = 0; i < packages.length; i++) {
			if (".".equals(packages[i].getName())) { //$NON-NLS-1$
				header.removePackage(packages[i]);
				return new ReplaceEdit(header.getOffset(), header.getLength(),
						header.write()); 
			}
		}
		return null;
	}

	protected void setPluginLibraries(WorkspacePluginModelBase model)
			throws CoreException {
		PluginBase pluginBase = (PluginBase) model.getPluginBase();
		if (fData.isUnzipLibraries()) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName("."); //$NON-NLS-1$
			library.setExported(true);
			pluginBase.add(library);

		} else {
			String[] paths = fData.getLibraryPaths();
			for (int i = 0; i < paths.length; i++) {
				File jarFile = new File(paths[i]);
				IPluginLibrary library = model.getPluginFactory()
						.createLibrary();
				library.setName(jarFile.getName());
				library.setExported(true);
				pluginBase.add(library);
			}
		}
	}

	protected void createSourceOutputBuildEntries(WorkspaceBuildModel model,
			IBuildModelFactory factory) throws CoreException {
		if (fData.isUnzipLibraries()) {
			// SOURCE.<LIBRARY_NAME>
			IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX
					+ "."); //$NON-NLS-1$
			entry.addToken("."); //$NON-NLS-1$
			model.getBuild().add(entry);

			// OUTPUT.<LIBRARY_NAME>
			entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX + "."); //$NON-NLS-1$
			entry.addToken("."); //$NON-NLS-1$
			model.getBuild().add(entry);
		}
	}

}
