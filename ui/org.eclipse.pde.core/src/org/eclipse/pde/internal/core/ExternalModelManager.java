/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.target.NameVersionDescriptor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class ExternalModelManager extends AbstractModelManager {

	private IPluginModelBase[] fModels = new IPluginModelBase[0];
	private ExternalLibraryCache fLibCache = new ExternalLibraryCache();

	protected IPluginModelBase[] getAllModels() {
		return fModels;
	}

	protected void initializeModels(IPluginModelBase[] models) {
		fModels = models;
		PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL)) {
			for (int i = 0; i < fModels.length; i++)
				fModels[i].setEnabled(true);
		} else if (!saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			String versionString = pref.getString(ICoreConstants.CHECKED_VERSION_PLUGINS);
			Set versions = new HashSet();
			Set versionIds = new HashSet();
			if (versionString != null && versionString.trim().length() > 0) {
				if (!versionString.equals(ICoreConstants.VALUE_SAVED_NONE)) {
					// restore version information, if any
					StringTokenizer stok = new StringTokenizer(versionString);
					while (stok.hasMoreTokens()) {
						NameVersionDescriptor desc = NameVersionDescriptor.fromPortableString(stok.nextToken());
						versions.add(desc);
						versionIds.add(desc.getId());
					}
				}
			}
			Vector result = new Vector();
			StringTokenizer stok = new StringTokenizer(saved);
			while (stok.hasMoreTokens()) {
				result.add(stok.nextToken());
			}
			for (int i = 0; i < fModels.length; i++) {
				String id = fModels[i].getPluginBase().getId();
				if (versionIds.contains(id)) {
					fModels[i].setEnabled(!versions.contains(new NameVersionDescriptor(id, fModels[i].getPluginBase().getVersion())));
				} else {
					fModels[i].setEnabled(!result.contains(id));
				}
			}
		}
		fLibCache.cleanExtractedLibraries(fModels);
	}

	public void setModels(IPluginModelBase[] models) {
		fModels = models;
		fLibCache.cleanExtractedLibraries(fModels);
	}

	/**
	 * Returns the URLs of all external plug-ins referenced by PDE target platform preferences.
	 * <p>
	 * Note this method is public for testing purposes only.
	 * </p>
	 * @return URLs of all external plug-ins referenced by PDE target platform preferences.
	 */
	public URL[] getPluginPaths() {
		PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
		boolean addPool = false;
		String baseLocation = pref.getString(ICoreConstants.PLATFORM_PATH);
		URL[] base = null;
		if (P2TargetUtils.BUNDLE_POOL.isPrefixOf(new Path(baseLocation))) {
			// if the base platform path is part of the bundle pool, use the bundle pool
			// preference info to restore bundles selectively
			addPool = true;
			base = new URL[0];
		} else {
			base = PluginPathFinder.getPluginPaths(baseLocation);
		}

		String value = pref.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$

		if (!addPool && tokenizer.countTokens() == 0)
			return base;

		List extraLocations = new ArrayList(tokenizer.countTokens());
		while (tokenizer.hasMoreTokens()) {
			String location = tokenizer.nextToken();
			if (P2TargetUtils.BUNDLE_POOL.isPrefixOf(new Path(location))) {
				addPool = true;
			} else {
				File dir = new File(location, "plugins"); //$NON-NLS-1$
				if (!dir.exists() || !dir.isDirectory())
					dir = new File(location);
				extraLocations.add(dir);
			}
		}
		URL[] additional = PluginPathFinder.scanLocations((File[]) extraLocations.toArray(new File[extraLocations.size()]));
		URL[] result = append(base, additional);

		// add pooled bundles (only if part of the profile)
		if (addPool) {
			String pooled = pref.getString(ICoreConstants.POOLED_URLS);
			if (pooled != null && pooled.trim().length() > 0) {
				if (ICoreConstants.VALUE_SAVED_NONE.equals(pooled)) {
					// none
				} else {
					tokenizer = new StringTokenizer(pooled, ","); //$NON-NLS-1$
					List urls = new ArrayList(tokenizer.countTokens());
					while (tokenizer.hasMoreTokens()) {
						String fileName = tokenizer.nextToken();
						try {
							urls.add(P2TargetUtils.BUNDLE_POOL.append("plugins").append(fileName).toFile().toURL()); //$NON-NLS-1$
						} catch (MalformedURLException e) {
							PDECore.log(e);
						}
					}
					additional = (URL[]) urls.toArray(new URL[urls.size()]);
					result = append(result, additional);
				}
			}
		}

		return result;
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
	 * Appends one {@link URL} to another
	 * 
	 * @param base first URL
	 * @param additional second URL
	 * @return a URL with the additional URL appended to the base
	 */
	private URL[] append(URL[] base, URL[] additional) {
		if (additional.length == 0) {
			return base;
		}
		URL[] result = new URL[base.length + additional.length];
		System.arraycopy(base, 0, result, 0, base.length);
		System.arraycopy(additional, 0, result, base.length, additional.length);
		return result;
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
		if (!fJarFile.isFile())
			return new File[0];

		BundleDescription desc = model.getBundleDescription();
		IPluginLibrary[] libs = model.getPluginBase().getLibraries();

		File fCacheDir = new File(getLibraryCacheDir(), getBundleLibsCacheDirName(desc));

		List files = new ArrayList();

		for (int i = 0; i < libs.length; i++) {
			String libName = libs[i].getName();
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
				} else
					files.add(fDestFile);
			}
		}

		return (File[]) files.toArray(new File[0]);
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
		if (!fCacheDir.isDirectory())
			return;

		// build a list with all potential directory names for quick check
		Set bundleKeys = new HashSet();

		for (int i = 0; i < targetModels.length; i++) {
			if (targetModels[i].isEnabled()) {
				BundleDescription desc = targetModels[i].getBundleDescription();
				bundleKeys.add(getBundleLibsCacheDirName(desc));
			}
		}

		File[] fDirs = fCacheDir.listFiles();
		for (int i = 0; i < fDirs.length; i++) {
			if (fDirs[i].isDirectory() && !bundleKeys.contains(fDirs[i].getName()))
				CoreUtility.deleteContent(fDirs[i]);
		}

		// Delete the cache folder if it is empty
		fCacheDir.delete();
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
		JarFile f = new JarFile(fJarFile);
		InputStream in = null;
		try {
			ZipEntry libEntry = f.getEntry(libName);
			if (libEntry == null || libEntry.isDirectory()) {
				return null;
			}
			fTargetFile.getParentFile().mkdirs();
			in = f.getInputStream(libEntry);
			if (in == null)
				throw new IOException();

			CoreUtility.readFile(in, fTargetFile);
			return fTargetFile;
		} finally {
			try {
				f.close();
			} catch (Exception e) {
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
			}
		}
	}

}
