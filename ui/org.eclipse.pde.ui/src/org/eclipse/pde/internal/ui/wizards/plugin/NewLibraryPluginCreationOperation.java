/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.ZipFile;

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
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.converter.PluginConverter;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.BundleException;
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

	private void adjustExportRoot(IProject project, IBundle bundle)
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
		removeExportRoot(bundle);
	}

	protected void adjustManifests(IProgressMonitor monitor, IProject project, IBundle bundle)
			throws CoreException {
		super.adjustManifests(monitor, project, bundle);
		monitor.beginTask(new String(), 2);
		if (bundle != null) {
			adjustExportRoot(project, bundle);
			monitor.worked(1);
			addExportedPackages(project, bundle);
		}
		monitor.done();
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
							IPDEUIConstants.PLUGIN_ID,
							IStatus.OK,
							NLS.bind(PDEUIMessages.NewProjectCreationOperation_errorImportingJar,jar), e));
		}
	}

	private void removeExportRoot(IBundle bundle) {
		String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
		if (value == null) 
			value = "."; //$NON-NLS-1$
		try {
			ManifestElement [] elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
			StringBuffer buff = new StringBuffer(value.length());
			for (int i = 0; i < elems.length; i++) {
				if (!elems[i].getValue().equals(".")) //$NON-NLS-1$
					buff.append(elems[i].getValue());
			}
			bundle.setHeader(Constants.BUNDLE_CLASSPATH, buff.toString());
		} catch (BundleException e) {
		}		
	}

	protected void setPluginLibraries(WorkspacePluginModelBase model)
			throws CoreException {
		IPluginBase pluginBase = model.getPluginBase();
		if (fData.isUnzipLibraries()) {
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName("."); //$NON-NLS-1$
			library.setExported(true);
			pluginBase.add(library);
		} else {
			String[] paths = fData.getLibraryPaths();
			for (int i = 0; i < paths.length; i++) {
				File jarFile = new File(paths[i]);
				IPluginLibrary library = model.getPluginFactory().createLibrary();
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
			IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX + "."); //$NON-NLS-1$
			entry.addToken("."); //$NON-NLS-1$
			model.getBuild().add(entry);

			// OUTPUT.<LIBRARY_NAME>
			entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX + "."); //$NON-NLS-1$
			entry.addToken("."); //$NON-NLS-1$
			model.getBuild().add(entry);		
		}
	}
	
	private void addExportedPackages(IProject project, IBundle bundle) {
		String value = bundle.getHeader(Constants.BUNDLE_CLASSPATH);
		if (value == null)
			value = "."; //$NON-NLS-1$
		try {
			ManifestElement[] elems = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, value);
			HashMap map = new HashMap();
			for (int i = 0; i < elems.length; i++) {
				ArrayList filter = new ArrayList();
				filter.add("*"); //$NON-NLS-1$
				map.put(elems[i].getValue(), filter);
			}
			Set packages = PluginConverter.getDefault().getExports(project, map);
			String pkgValue = getCommaValueFromSet(packages);
			bundle.setHeader(Constants.EXPORT_PACKAGE, pkgValue);
		} catch (BundleException e) {
		}
	}

}
