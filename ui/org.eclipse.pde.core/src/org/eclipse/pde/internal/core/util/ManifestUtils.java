/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.target.Messages;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ManifestUtils {

	private ManifestUtils() {
	}

	public static IPackageFragmentRoot[] findPackageFragmentRoots(IManifestHeader header, IProject project) {
		IJavaProject javaProject = JavaCore.create(project);

		String[] libs;
		if (header == null || header.getValue() == null)
			libs = new String[] {"."}; //$NON-NLS-1$
		else
			libs = header.getValue().split(","); //$NON-NLS-1$

		IBuild build = getBuild(project);
		if (build == null) {
			try {
				return javaProject.getPackageFragmentRoots();
			} catch (JavaModelException e) {
				return new IPackageFragmentRoot[0];
			}
		}
		List pkgFragRoots = new LinkedList();
		for (int j = 0; j < libs.length; j++) {
			String lib = libs[j];
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=230469  			
			IPackageFragmentRoot root = null;
			if (!lib.equals(".")) { //$NON-NLS-1$
				try {
					root = javaProject.getPackageFragmentRoot(project.getFile(lib));
				} catch (IllegalArgumentException e) {
					return new IPackageFragmentRoot[0];
				}
			}
			if (root != null && root.exists()) {
				pkgFragRoots.add(root);
			} else {
				IBuildEntry entry = build.getEntry("source." + lib); //$NON-NLS-1$
				if (entry == null)
					continue;
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IResource resource = project.findMember(tokens[i]);
					if (resource == null)
						continue;
					root = javaProject.getPackageFragmentRoot(resource);
					if (root != null && root.exists())
						pkgFragRoots.add(root);
				}
			}
		}
		return (IPackageFragmentRoot[]) pkgFragRoots.toArray(new IPackageFragmentRoot[pkgFragRoots.size()]);
	}

	public final static IBuild getBuild(IProject project) {
		IFile buildProps = PDEProject.getBuildProperties(project);
		if (buildProps.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			return model.getBuild();
		}
		return null;
	}

	public static boolean isImmediateRoot(IPackageFragmentRoot root) throws JavaModelException {
		int kind = root.getKind();
		return kind == IPackageFragmentRoot.K_SOURCE || (kind == IPackageFragmentRoot.K_BINARY && !root.isExternal());
	}

	/**
	 * Utility method to parse a bundle's manifest into a dictionary. The bundle may be in 
	 * a directory or an archive at the specified location.  If the manifest does not contain
	 * the necessary entries, the plugin.xml and fragment.xml will be checked for a non-osgi plug-in.
	 * 
	 * TODO This method may be removed in favour of one that caches manifest contents
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary
	 * @throws CoreException if manifest has invalid syntax or is missing
	 */
	public static Map loadManifest(File bundleLocation) throws CoreException {
		// Check if the file is a archive or a directory
		try {
			if (bundleLocation.isFile()) {
				ZipFile jarFile = null;
				InputStream stream = null;
				try {
					jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
					// Check the manifest.MF
					ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
					if (manifestEntry != null) {
						stream = jarFile.getInputStream(manifestEntry);
						if (stream != null) {
							Map map = ManifestElement.parseBundleManifest(stream, new Hashtable(10));
							// Symbolic name is the only required manifest entry, this is an ok bundle
							if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
								return map;
							}
						}
						// Manifest.MF wasn't good, check for plugin.xml or fragment.xml
						ZipEntry pluginEntry = jarFile.getEntry(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
						if (pluginEntry == null) {
							pluginEntry = jarFile.getEntry(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
						}
						if (pluginEntry != null) {
							Map map = loadPluginXML(bundleLocation);
							if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
								return map;
							}
						}
					}
				} finally {
					closeZipFileAndStream(stream, jarFile);
				}
			} else {
				// Check the manifest.MF
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					InputStream stream = null;
					try {
						stream = new FileInputStream(file);
						Map map = ManifestElement.parseBundleManifest(stream, new Hashtable(10));
						if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
							return map;
						}
					} finally {
						if (stream != null) {
							stream.close();
						}
					}
				}
				// Manifest.MF wasn't good, check for plugin.xml or fragment.xml
				File pxml = new File(bundleLocation, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
				File fxml = new File(bundleLocation, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
				if (pxml.exists() || fxml.exists()) {
					Map map = loadPluginXML(bundleLocation);
					if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
						return map;
					}
				}
			}

			// The necessary bundle information has not been found in manifest.mf or plugin.xml
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));

		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		}
	}

	/**
	 * Parses an old style plug-in's (or fragment's) XML definition file into a dictionary.
	 * The provided file may be an zip archive or directory.  The existence of a plugin.xml
	 * or fragment.xml is not checked in this method.
	 * 
	 * @param pluginLocation location of the bundle (archive file or directory)
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if manifest has invalid syntax
	 */
	private static Map loadPluginXML(File pluginLocation) throws CoreException {
		PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
		if (converter != null) {
			try {
				Dictionary convert = converter.convertManifest(pluginLocation, false, null, false, null);
				if (convert != null) {
					Map map = new HashMap(convert.size(), 1.0f);
					Enumeration keys = convert.keys();
					while (keys.hasMoreElements()) {
						Object key = keys.nextElement();
						map.put(key, convert.get(key));
					}
					return map;
				}
			} catch (PluginConversionException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_2, pluginLocation.getAbsolutePath()), e));
			}
		}
		return null;
	}

	/**
	 * Closes the stream and file
	 * @param stream
	 * @param jarFile
	 */
	private static void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

}
