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
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePlugin;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.Plugin;

public class ClasspathUtilCore {
	
	public static IClasspathEntry createContainerEntry() {
		return JavaCore.newContainerEntry(new Path(PDECore.CLASSPATH_CONTAINER_ID));
	}

	public static IClasspathEntry createJREEntry() {
		return JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")); //$NON-NLS-1$
	}
	
	public static void addLibraries(IPluginModelBase model, boolean isExported, boolean useInclusionPatterns, ArrayList result) throws CoreException {
		if (new File(model.getInstallLocation()).isFile()) {
			addJARdPlugin(model, isExported, useInclusionPatterns, result);
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				if (IPluginLibrary.RESOURCE.equals(libraries[i].getType()))
					continue;
				IClasspathEntry entry = createLibraryEntry(libraries[i], isExported, useInclusionPatterns);
				if (entry != null && !result.contains(entry)) {
					result.add(entry);
				}
			}		
		}
	}
	
	private static void addJARdPlugin(IPluginModelBase model,
			boolean isExported, boolean useInclusionPatterns, ArrayList result)
			throws CoreException {
		
		IPath sourcePath = getSourceAnnotation(model, "."); //$NON-NLS-1$
		if (sourcePath == null)
			sourcePath = new Path(model.getInstallLocation());
		
		IClasspathEntry entry  = JavaCore.newLibraryEntry(
						new Path(model.getInstallLocation()), 
						sourcePath, 
						null, 
						isExported);
		if (entry != null && !result.contains(entry)) {
			result.add(entry);
		}
	}

	protected static IClasspathEntry createLibraryEntry(
		IPluginLibrary library,
		boolean exported,
		boolean useInclusionPatterns) {
		
		IClasspathEntry entry = null;
		try {
			String name = library.getName();
			String expandedName = expandLibraryName(name);

			IPluginModelBase model = library.getPluginModel();
			IPath path = getPath(model, expandedName);
			if (path == null) {
				if (model.isFragmentModel() || !containsVariables(name))
					return null;
				model = resolveLibraryInFragments(library, expandedName);
				if (model == null)
					return null;
				path = getPath(model, expandedName);
			}
			

				entry = JavaCore.newLibraryEntry(
						path, 
						getSourceAnnotation(model, expandedName),
						null, 
						exported);
		
		} catch (CoreException e) {
		}
		return entry;
	}
	
	public static boolean hasExtensibleAPI(IPlugin plugin) {
		if (plugin instanceof Plugin)
			return ((Plugin) plugin).hasExtensibleAPI();
		if (plugin instanceof BundlePlugin)
			return ((BundlePlugin) plugin).hasExtensibleAPI();
		return false;
	}
		
	public static boolean isBundle(IPluginModelBase model) {
		if (model instanceof IBundlePluginModelBase)
			return true;
		if (model.getUnderlyingResource() == null) {
			File file = new File(model.getInstallLocation());
			if (file.isDirectory())
				return new File(file, "META-INF/MANIFEST.MF").exists();
			ZipFile jarFile = null;
			try {
				jarFile = new ZipFile(file, ZipFile.OPEN_READ);
				return jarFile.getEntry("META-INF/MANIFEST.MF") != null;
			} catch (IOException e) {
			} finally {
				try {
					if (jarFile != null)
						jarFile.close();
				} catch (IOException e) {
				}
			}
		}
		return false;
	}
	
	public static boolean containsVariables(String name) {
		return name.indexOf("$os$") != -1 //$NON-NLS-1$
			|| name.indexOf("$ws$") != -1 //$NON-NLS-1$
			|| name.indexOf("$nl$") != -1 //$NON-NLS-1$
			|| name.indexOf("$arch$") != -1; //$NON-NLS-1$
	}

	public static String expandLibraryName(String source) {
		if (source == null || source.length() == 0)
			return ""; //$NON-NLS-1$
		if (source.indexOf("$ws$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
					"\\$ws\\$", //$NON-NLS-1$
					"ws" + IPath.SEPARATOR + TargetPlatform.getWS()); //$NON-NLS-1$
		if (source.indexOf("$os$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
					"\\$os\\$", //$NON-NLS-1$
					"os" + IPath.SEPARATOR + TargetPlatform.getOS()); //$NON-NLS-1$
		if (source.indexOf("$nl$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
						"\\$nl\\$", //$NON-NLS-1$
						"nl" + IPath.SEPARATOR + TargetPlatform.getNL()); //$NON-NLS-1$
		if (source.indexOf("$arch$") != -1) //$NON-NLS-1$
			source =
				source.replaceAll(
						"\\$arch\\$", //$NON-NLS-1$
						"arch" + IPath.SEPARATOR + TargetPlatform.getOSArch()); //$NON-NLS-1$
		return source;
	}

	public static IPath getSourceAnnotation(IPluginModelBase model, String libraryName)
		throws CoreException {
		IPath path = null;
		int dot = libraryName.lastIndexOf('.');
		if (dot != -1) {
			String zipName = libraryName.substring(0, dot) + "src.zip"; //$NON-NLS-1$
			path = getPath(model, zipName);
			if (path == null) {
				SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
				path = manager.findSourcePath(model.getPluginBase(), new Path(zipName));
			}
		}
		return path;
	}

	private static IPluginModelBase resolveLibraryInFragments(
		IPluginLibrary library,
		String libraryName) {
		IFragment[] fragments =
			PDECore.getDefault().findFragmentsFor(
				library.getPluginBase().getId(),
				library.getPluginBase().getVersion());

		for (int i = 0; i < fragments.length; i++) {
			IPath path = getPath(fragments[i].getPluginModel(), libraryName);
			if (path != null)
				return fragments[i].getPluginModel();
			
			// the following case is to account for cases when a plugin like org.eclipse.swt.win32 is checked out from
			// cvs (i.e. it has no library) and org.eclipse.swt is not in the workspace.
			// we have to find the external swt.win32 fragment to locate the $ws$/swt.jar.
			if (fragments[i].getModel().getUnderlyingResource() != null) {
				ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(fragments[i].getId());
				IPluginModelBase model = entry.getExternalModel();
				if (model != null && model instanceof IFragmentModel) {
					path = getPath(model, libraryName);
					if (path != null)
						return model;
				}
			}
		}
		return null;
	}

	private static IPath getPath(IPluginModelBase model, String libraryName) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IResource jarFile = resource.getProject().findMember(libraryName);
			if (jarFile != null)
				return jarFile.getFullPath();
		} else {
			File file = new File(model.getInstallLocation(), libraryName);
			if (file.exists())
				return new Path(file.getAbsolutePath());
		}
		return null;
	}
	
	protected static IBuild getBuild(IPluginModelBase model)
			throws CoreException {
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel == null) {
			IProject project = model.getUnderlyingResource().getProject();
			IFile buildFile = project.getFile("build.properties"); //$NON-NLS-1$
			if (buildFile.exists()) {
				buildModel = new WorkspaceBuildModel(buildFile);
				buildModel.load();
			}
		}
		return (buildModel != null) ? buildModel.getBuild() : null;
	}

}
