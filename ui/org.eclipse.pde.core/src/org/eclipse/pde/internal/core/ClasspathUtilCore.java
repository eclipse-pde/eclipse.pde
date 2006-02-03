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
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundleFragment;
import org.eclipse.pde.internal.core.bundle.BundlePlugin;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.Fragment;
import org.eclipse.pde.internal.core.plugin.Plugin;
import org.eclipse.pde.internal.core.plugin.PluginBase;

public class ClasspathUtilCore {
	
	public static void addLibraries(IPluginModelBase model, ArrayList result) throws CoreException {
		if (new File(model.getInstallLocation()).isFile()) {
			addJARdPlugin(model, result);
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				if (IPluginLibrary.RESOURCE.equals(libraries[i].getType()))
					continue;
				IClasspathEntry entry = createLibraryEntry(libraries[i]);
				if (entry != null && !result.contains(entry)) {
					result.add(entry);
				}
			}		
		}
	}
	
	private static void addJARdPlugin(IPluginModelBase model, ArrayList result)
			throws CoreException {
		
		IPath sourcePath = getSourceAnnotation(model, "."); //$NON-NLS-1$
		if (sourcePath == null)
			sourcePath = new Path(model.getInstallLocation());
		
		IClasspathEntry entry  = JavaCore.newLibraryEntry(
						new Path(model.getInstallLocation()), 
						sourcePath, 
						null, 
						false);
		if (entry != null && !result.contains(entry)) {
			result.add(entry);
		}
	}

	protected static IClasspathEntry createLibraryEntry(IPluginLibrary library) {
		
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
			
			// classpath must not contain entries referencing external folders
			if (model.getUnderlyingResource() == null && path.toFile().isDirectory())
				return null;
			
			entry = JavaCore.newLibraryEntry(
					path, 
					getSourceAnnotation(model, expandedName),
					null, 
					false);		
		} catch (CoreException e) {
		}
		return entry;
	}
	
	public static boolean hasExtensibleAPI(IPluginModelBase model) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IPlugin)
			return hasExtensibleAPI((IPlugin)pluginBase);
		return false;
	}
	
	public static boolean isPatchFragment(BundleDescription desc) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IFragmentModel model = manager.findFragmentModel(desc.getSymbolicName());
		return model != null ? isPatchFragment(model.getFragment()) : false;
	}
	
	public static boolean isPatchFragment(IPluginModelBase model) {
		IPluginBase pluginBase = model.getPluginBase();
		if (pluginBase instanceof IFragment)
			return isPatchFragment((IFragment)pluginBase);
		return false;
	}
	
	private static boolean hasExtensibleAPI(IPlugin plugin) {
		if (plugin instanceof Plugin)
			return ((Plugin) plugin).hasExtensibleAPI();
		if (plugin instanceof BundlePlugin)
			return ((BundlePlugin) plugin).hasExtensibleAPI();
		return false;
	}
		
	private static boolean isPatchFragment(IFragment fragment) {
		if (fragment instanceof Fragment) 
			return ((Fragment)fragment).isPatch();
		if (fragment instanceof BundleFragment)
			return ((BundleFragment)fragment).isPatch();
		return false;
	}
	
	public static boolean hasBundleStructure(IPluginModelBase model) {
		if (model.getUnderlyingResource() != null)
			return model instanceof IBundlePluginModelBase;
		
		IPluginBase plugin = model.getPluginBase();
		if (plugin instanceof PluginBase) 
			return ((PluginBase)plugin).hasBundleStructure();
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
		String zipName = getSourceZipName(libraryName);
		IPath path = getPath(model, zipName);
		if (path == null) {
			SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
			path = manager.findSourcePath(model.getPluginBase(), new Path(zipName));
		}
		return path;
	}
	
	public static String getSourceZipName(String libraryName) {
		int dot = libraryName.lastIndexOf('.');
		return (dot != -1) ? libraryName.substring(0, dot) + "src.zip" : libraryName;	 //$NON-NLS-1$
	}

	private static IPluginModelBase resolveLibraryInFragments(
		IPluginLibrary library,
		String libraryName) {
		IFragmentModel[] fragments =
			PDEManager.findFragmentsFor(library.getPluginModel());

		for (int i = 0; i < fragments.length; i++) {
			IPath path = getPath(fragments[i], libraryName);
			if (path != null)
				return fragments[i];
			
			// the following case is to account for cases when a plugin like org.eclipse.swt.win32 is checked out from
			// cvs (i.e. it has no library) and org.eclipse.swt is not in the workspace.
			// we have to find the external swt.win32 fragment to locate the $ws$/swt.jar.
			if (fragments[i].getUnderlyingResource() != null) {
				ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(fragments[i].getFragment().getId());
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
	
	public static IBuild getBuild(IPluginModelBase model)
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
	
	public static String getFilename(IPluginModelBase model) {
		StringBuffer buffer = new StringBuffer();
		String id = model.getPluginBase().getId();
		if (id != null)
			buffer.append(id);
		buffer.append("_"); //$NON-NLS-1$
		String version = model.getPluginBase().getVersion();
		if (version != null)
			buffer.append(version);
		buffer.append(".jar"); //$NON-NLS-1$
		return buffer.toString();
	}
	
}
