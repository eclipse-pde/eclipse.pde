/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.project;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Utility class to resolve plug-in and bundle files relative to a project 
 * specific bundle root location.
 * 
 * @since 3.6
 */
public class PDEProject {

	/**
	 * Preference key for the project relative bundle root path
	 */
	public static final String BUNDLE_ROOT_PATH = "BUNDLE_ROOT_PATH"; //$NON-NLS-1$

	/**
	 * Returns the container in the specified project that corresponds to the
	 * root of bundle related artifacts. May return the project itself
	 * or a folder within the project.
	 * 
	 * @param project project
	 * @return container corresponding to the bundle root
	 */
	public static IContainer getBundleRoot(IProject project) {
		ProjectScope scope = new ProjectScope(project);
		IEclipsePreferences node = scope.getNode(PDECore.PLUGIN_ID);
		if (node != null) {
			String string = node.get(BUNDLE_ROOT_PATH, null);
			if (string != null) {
				IPath path = Path.fromPortableString(string);
				return project.getFolder(path);
			}
		}
		return project;
	}

	/**
	 * Returns the launch shortcuts configured for this project
	 * or <code>null</code> if default launchers should be used.
	 *  
	 * @param project project
	 * @return configured launch shortcuts or <code>null</code>
	 */
	public static String[] getLaunchShortcuts(IProject project) {
		ProjectScope scope = new ProjectScope(project);
		IEclipsePreferences node = scope.getNode(PDECore.PLUGIN_ID);
		if (node != null) {
			String list = node.get(ICoreConstants.MANIFEST_LAUNCH_SHORTCUTS, (String) null);
			if (list != null) {
				return list.split(","); //$NON-NLS-1$
			}
		}
		return null;
	}

	/**
	 * Returns the export wizard configured for this project or <code>null</code>
	 * if default.
	 * 
	 * @param project project
	 * @return export wizard identifier or <code>null</code>
	 */
	public static String getExportWizard(IProject project) {
		ProjectScope scope = new ProjectScope(project);
		IEclipsePreferences node = scope.getNode(PDECore.PLUGIN_ID);
		if (node != null) {
			return node.get(ICoreConstants.MANIFEST_EXPORT_WIZARD, (String) null);
		}
		return null;
	}

	/**
	 * Sets the root of the bundle related artifacts in the specified project
	 * to the specified container. When <code>null</code> is specified, the 
	 * bundle root will be the project itself. The container must be within
	 * the specified project.
	 * 
	 * @param project project
	 * @param root project relative bundle root path, or <code>null</code> (or an empty path)
	 *  to indicate the root of the bundle is the root of the project
	 * @exception CoreException if unable to set the bundle root to the specified container
	 */
	public static void setBundleRoot(IProject project, IContainer root) throws CoreException {
		if (root != null && !root.getProject().equals(project)) {
			throw new IllegalArgumentException("root must be contained in the given project"); //$NON-NLS-1$
		}
		ProjectScope scope = new ProjectScope(project);
		IEclipsePreferences node = scope.getNode(PDECore.PLUGIN_ID);
		if (node != null) {
			IPath path = null;
			if (root != null) {
				path = root.getProjectRelativePath();
			}
			if (path != null && path.isEmpty()) {
				path = null;
			}
			String value = null;
			if (path != null) {
				value = path.toPortableString();
			}
			if (value == null) {
				node.remove(BUNDLE_ROOT_PATH);
			} else {
				node.put(BUNDLE_ROOT_PATH, value);
			}
			try {
				node.flush();
				// update model manager
				PDECore.getDefault().getModelManager().bundleRootChanged(project);
			} catch (BackingStoreException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, e.getMessage(), e));
			}
		} else {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Failed to retrieve project scope preference settings")); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>MANIFEST.MF</code> file.
	 * 
	 * @param project project
	 * @return <code>MANIFEST.MF</code> file that may or may not exist
	 */
	public static IFile getManifest(IProject project) {
		return getBundleRelativeFile(project, ICoreConstants.MANIFEST_PATH);
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>build.properties</code>file.
	 * 
	 * @param project project
	 * @return <code>build.properties</code> file that may or may not exist
	 */
	public static IFile getBuildProperties(IProject project) {
		return getBundleRelativeFile(project, ICoreConstants.BUILD_PROPERTIES_PATH);
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>plugin.xml</code>file.
	 * 
	 * @param project project
	 * @return <code>plugin.xml</code> file that may or may not exist
	 */
	public static IFile getPluginXml(IProject project) {
		return getBundleRelativeFile(project, ICoreConstants.PLUGIN_PATH);
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>fragment.xml</code>file.
	 * 
	 * @param project project
	 * @return <code>fragment.xml</code> file that may or may not exist
	 */
	public static IFile getFragmentXml(IProject project) {
		return getBundleRelativeFile(project, ICoreConstants.FRAGMENT_PATH);
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>feature.xml</code>file.
	 * 
	 * @param project project
	 * @return <code>feature.xml</code> file that may or may not exist
	 */
	public static IFile getFeatureXml(IProject project) {
		return getBundleRelativeFile(project, ICoreConstants.FEATURE_PATH);
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>.options</code>file.
	 * 
	 * @param project project
	 * @return <code>.options</code> file that may or may not exist
	 */
	public static IFile getOptionsFile(IProject project) {
		return getBundleRelativeFile(project, new Path(ICoreConstants.OPTIONS_FILENAME));
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>OSGI-INF/</code>folder.
	 * 
	 * @param project project
	 * @return <code>OSGI-INF/</code> folder that may or may not exist
	 */
	public static IFolder getOSGiInf(IProject project) {
		return getBundleRelativeFolder(project, ICoreConstants.OSGI_INF_PATH);
	}

	/**
	 * Returns the resource in the specified project corresponding to its
	 * <code>META-INF/</code>folder.
	 * 
	 * @param project project
	 * @return <code>META-INF/</code> folder that may or may not exist
	 */
	public static IFolder getMetaInf(IProject project) {
		return getBundleRelativeFolder(project, new Path(ICoreConstants.MANIFEST_FOLDER_NAME));
	}

	/**
	 * Returns a file relative to the bundle root of the specified project.
	 * 
	 * @param project project
	 * @param path bundle root relative path
	 * @return file that may or may not exist
	 */
	public static IFile getBundleRelativeFile(IProject project, IPath path) {
		return getBundleRoot(project).getFile(path);
	}

	/**
	 * Returns a folder relative to the bundle root of the specified project.
	 * 
	 * @param project project
	 * @param path bundle root relative path
	 * @return folder that may or may not exist
	 */
	public static IFolder getBundleRelativeFolder(IProject project, IPath path) {
		return getBundleRoot(project).getFolder(path);
	}

	/**
	 * Returns the bundle localization file for the specified bundle project.
	 * The file may or may not exist.
	 * 
	 * @param project
	 * @return bunlde localization file which may or may not exist
	 */
	public static IFile getLocalizationFile(IProject project) {
		IPluginModelBase model = PluginRegistry.findModel(project);
		String localization = PDEManager.getBundleLocalization(model);
		return getBundleRelativeFile(project, new Path(localization + ".properties")); //$NON-NLS-1$
	}

	// TODO: schema folder?

	// TODO: plugin_customization.ini ?

}
