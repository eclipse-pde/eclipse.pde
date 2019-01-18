/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class ExternalModelManager extends AbstractModelManager {

	private IPluginModelBase[] fModels = new IPluginModelBase[0];
	private final ExternalLibraryCache fLibCache = new ExternalLibraryCache();

	public IPluginModelBase[] getAllModels() {
		return fModels;
	}

	public void setModels(IPluginModelBase[] models) {
		fModels = models;
		fLibCache.cleanExtractedLibraries(fModels);
	}

	/**
	 * Returns all libraries extracted from an external jarred plug-in.  Will return an empty
	 * array if the plug-in is not jarred or if no jarred libraries exist inside it.
	 * <p>
	 * Previously cached libraries will be returned.  Any libraries not found in the cache will
	 * extracted from the plug-in and placed in PDE's metadata location.
	 * </p>
	 * @param model model to get the libraries for
	 * @return all extracted libraries or an empty array
	 */
	public File[] getExtractedLibraries(IPluginModelBase model) {
		return fLibCache.getExtractedLibraries(model);
	}

	/**
	 * Returns the path of a nested library jar resolved against the external
	 * model's {@link IPluginModelBase#getInstallLocation() install location}, or
	 * {@code null} if no jar exists at that location.
	 * <p>
	 * If the external model is a jarred plugin, the library jar is extracted and
	 * cached in PDE's metadata location; a previously cached library will be
	 * returned if it already exists in the cache. Otherwise, if the external model
	 * is a folder, the location of the jar within that folder is returned.
	 * </p>
	 *
	 * @param model
	 *            the model from which to extract the library.
	 * @param path
	 *            the path of the library relative to the model's install location.
	 * @return the path of a library jar resolved against the external model's
	 *         install location, or {@code null} if no jar exists at that location.
	 */
	public IPath getNestedLibrary(IPluginModelBase model, String path) {
		return fLibCache.getNestedLibrary(model, path);
	}

}

/**
 * When an external model is added to the classpath its libraries as defined by the bundle-classpath
 * header also need to be added to the classpath for the JDT compiler.  This is handled by the
 * {@link PDEClasspathContainer}.  However, because the classpath does not support nested jars, we
 * must extract any libraries from within a jarred bundle.  This class manages the set of libraries
 * that we have extracted and deletes them when the list of external models changes.
 *
 * @see PDEClasspathContainer#addExternalPlugin(IPluginModelBase, org.eclipse.pde.internal.core.PDEClasspathContainer.Rule[], ArrayList)
 * @since 3.7
 */
class ExternalLibraryCache {

	/**
	 * Location inside the PDE metadata area where extracted libraries will be stored.
	 * Extracted libraries will be stored under a directory named from the plug-in it
	 * was extracted from.
	 * <p>
	 * [workspace]/.metadata/.plugins/org.eclipse.pde.core/.external_libraries/[plugin_name]_[plugin_version]/[library_name].jar
	 * </p>
	 */
	private static final String LIB_CACHE_DIR = ".external_libraries"; //$NON-NLS-1$

	/**
	 * Returns all libraries extracted from an external jarred plug-in.  Will return an empty
	 * array if the plug-in is not jarred or if no jarred libraries exist inside it.
	 * <p>
	 * Previously cached libraries will be returned.  Any libraries not found in the cache will
	 * extracted from the plug-in and placed in PDE's metadata location.
	 * </p>
	 * @param model model to get the libraries for
	 * @return all extracted libraries or an empty array
	 */
	public File[] getExtractedLibraries(IPluginModelBase model) {
		File fJarFile = new File(model.getInstallLocation());
		if (!fJarFile.isFile()) {
			return new File[0];
		}

		BundleDescription desc = model.getBundleDescription();
		IPluginLibrary[] libs = model.getPluginBase().getLibraries();

		File fCacheDir = new File(getLibraryCacheDir(), getBundleLibsCacheDirName(desc));

		List<File> files = new ArrayList<>();

		for (IPluginLibrary lib : libs) {
			String libName = lib.getName();
			if (!".".equals(libName)) { //$NON-NLS-1$
				libName = ClasspathUtilCore.expandLibraryName(libName);
				File fDestFile = new File(fCacheDir, libName);
				// assume that an existing file is always valid
				if (!fDestFile.isFile()) {
					try {
						File extractedLib = extractJar(fJarFile, libName, fDestFile);
						if (extractedLib != null) {
							files.add(extractedLib);
						}
					} catch (IOException ie) {
						// do not add file, but log error
						PDECore.logException(ie, "Could not extract library from jarred bundle " + desc.getSymbolicName()); //$NON-NLS-1$
					}
				} else {
					files.add(fDestFile);
				}
			}
		}

		return files.toArray(new File[0]);
	}

	/**
	 * Deletes all the cached JARs of libraries which are currently not contained
	 * or enabled in the target platform. Will ignore any errors when trying to
	 * delete a directory.
	 *
	 * @param targetModels The current contents of the target platform.
	 */
	public void cleanExtractedLibraries(IPluginModelBase[] targetModels) {
		File fCacheDir = getLibraryCacheDir();
		if (!fCacheDir.isDirectory()) {
			return;
		}

		// build a list with all potential directory names for quick check
		Set<String> bundleKeys = new HashSet<>();

		for (IPluginModelBase targetModel : targetModels) {
			if (targetModel.isEnabled()) {
				BundleDescription desc = targetModel.getBundleDescription();
				bundleKeys.add(getBundleLibsCacheDirName(desc));
			}
		}

		File[] fDirs = fCacheDir.listFiles();
		for (int i = 0; i < fDirs.length; i++) {
			if (fDirs[i].isDirectory() && !bundleKeys.contains(fDirs[i].getName())) {
				CoreUtility.deleteContent(fDirs[i]);
			}
		}

		// Delete the cache folder if it is empty
		fCacheDir.delete();
	}

	/**
	 * Returns the path of a nested library jar resolved against the external
	 * model's {@link IPluginModelBase#getInstallLocation() install location}, or
	 * {@code null} if no jar exists at that location.
	 * <p>
	 * If the external model is a jarred plugin, the library jar is extracted and
	 * cached in PDE's metadata location; a previously cached library will be
	 * returned if it already exists in the cache. Otherwise, if the external model
	 * is a folder, the location of the jar within that folder is returned.
	 * </p>
	 *
	 * @param model
	 *            the model from which to extract the library.
	 * @param path
	 *            the path of the library relative to the model's install location.
	 * @return the path of a library jar resolved against the external model's
	 *         install location, or {@code null} if no jar exists at that location.
	 */
	public IPath getNestedLibrary(IPluginModelBase model, String path) {
		String installLocation = model.getInstallLocation();
		if (installLocation != null) {
			File location = new File(installLocation);
			if (location.isDirectory()) {
				File result = new File(location, path);
				return result.exists() ? new Path(result.getAbsolutePath()) : null;
			}

			if (location.isFile()) {
				BundleDescription desc = model.getBundleDescription();
				File fCacheDir = new File(getLibraryCacheDir(), getBundleLibsCacheDirName(desc));
				File fDestFile = new File(fCacheDir, path);
				synchronized (this) {
					if (!fDestFile.exists()) {
						File extractedLib = null;
						try {
							extractedLib = extractJar(location, path, fDestFile);
						} catch (IOException e) {
						}
						// If the library can't be extracted for any reason, create an empty file as a
						// marker to avoid repeatedly trying to extract the library.
						if (extractedLib == null) {
							try {
								fDestFile.getParentFile().mkdirs();
								fDestFile.createNewFile();
							} catch (IOException e) {
							}
						}
					}
				}
				// Only return the resolved path if the destination file is non-empty, i.e.,
				// return null for the empty marker file.
				return fDestFile.length() == 0 ? null : new Path(fDestFile.getAbsolutePath());
			}
		}

		return null;
	}

	/**
	 * @return The directory in the PDE Core's state location where wrapped JARs
	 * from external bundles are stored.
	 */
	private File getLibraryCacheDir() {
		IPath path = PDECore.getDefault().getStateLocation();
		return new File(path.toFile(), LIB_CACHE_DIR);
	}

	/**
	 * Returns the name of the library cache directory for the given bundle.
	 *
	 * @param desc Bundle descriptor.
	 *
	 * @return <code>[bundle ID]_[bundle version]</code>
	 */
	private String getBundleLibsCacheDirName(BundleDescription desc) {
		return desc.getSymbolicName() + "_" + desc.getVersion(); //$NON-NLS-1$
	}

	/**
	 * Extracts a library from a jarred plug-in to the specified directory.
	 *
	 * @param fJarFile jar file to extract from
	 * @param libName name of the library to extract
	 * @param fTargetFile file location to extract the library to
	 * @return the file where the jar is extracted if successful, <code>null</code> otherwise.
	 * @throws IOException
	 */
	private File extractJar(File fJarFile, String libName, File fTargetFile) throws IOException {
		try (JarFile f = new JarFile(fJarFile)) {
			ZipEntry libEntry = f.getEntry(libName);
			if (libEntry == null || libEntry.isDirectory()) {
				return null;
			}
			fTargetFile.getParentFile().mkdirs();
			try (InputStream in = f.getInputStream(libEntry)) {
			if (in == null) {
				throw new IOException();
			}

			CoreUtility.readFile(in, fTargetFile);
			}
			return fTargetFile;
		}
	}

}
