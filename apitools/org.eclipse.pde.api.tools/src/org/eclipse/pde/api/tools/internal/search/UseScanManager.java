/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.internal.core.OverflowingLRUCache;
import org.eclipse.jdt.internal.core.util.LRUCache;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.FileManager;
import org.eclipse.pde.api.tools.internal.util.Util;

public class UseScanManager {

	private static UseScanCache fApiComponentCache;
	private static UseScanManager fUseScanProcessor;
	private static String tempLocation = "${workspace_loc}/.metadata/.plugins/" + ApiPlugin.PLUGIN_ID + "/ApiUseScans/"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final String STATE_DELIM = "*"; //$NON-NLS-1$
	public static final String LOCATION_DELIM = "|"; //$NON-NLS-1$
	public static final String ESCAPE_REGEX = "\\"; //$NON-NLS-1$
	/**
	 * Regular expression for finding use scan directories
	 * <br><br>
	 * Value is: <code>^.* \\(.*\\)$</code>
	 */
	public static final Pattern NAME_REGEX = Pattern.compile("^.* \\(.*\\)$"); //$NON-NLS-1$
	/**
	 * Number of entries to cache in the {@link UseScanCache}
	 */
	public static final int DEFAULT_CACHE_SIZE = 1000;
	
	/**
	 * Cache to maintain the list of least recently used <code>UseScanReferences</code>
	 */
	private static class UseScanCache extends OverflowingLRUCache {

		public UseScanCache(int size) {
			super(size);
		}

		public UseScanCache(int size, int overflow) {
			super(size, overflow);
		}

		protected boolean close(LRUCacheEntry entry) {
			IReferenceCollection references = (IReferenceCollection) entry.value;
			references.clear();
			return true;
		}

		protected LRUCache newInstance(int size, int newOverflow) {
			return new UseScanCache(size, newOverflow);
		}

	}

	private String[] fLocations = null;

	/**
	 * {@link FileFilter} for finding use scan directories
	 */
	static FileFilter USESCAN_FILTER = new FileFilter() {
		public boolean accept(File pathname) {
			if(NAME_REGEX.matcher(pathname.getName()).matches()) {
				throw new RuntimeException(pathname.getName());
			}
			return false;
		}
	};
	
	//Singleton
	private UseScanManager() {
	}

	/**
	 * Returns a handle to the singleton instance
	 * 
	 * @return the singleton {@link UseScanManager}
	 */
	public synchronized static UseScanManager getInstance() {
		if (fUseScanProcessor == null) {
			fUseScanProcessor = new UseScanManager();
			fApiComponentCache = new UseScanCache(DEFAULT_CACHE_SIZE);
		}
		return fUseScanProcessor;
	}

	/**
	 * Returns the references for a given <code>IApiComponent</code>. If it can not find them in cache, they will be fetched from the 
	 * API Use Scans and stored.
	 * @param apiComponent component whose references have to be fetched
	 * @param refTypes reference types for which the references has to be computed in the  given <code>IApiComponent</code>. 
	 * If <code>null</code> or empty, all references will be returned
	 * @param monitor
	 * @return the array of reference descriptors
	 */
	public IReferenceDescriptor[] getExternalDependenciesFor(IApiComponent apiComponent, String[] apiUseTypes, IProgressMonitor monitor) {
		IReferenceCollection references = (IReferenceCollection) fApiComponentCache.get(apiComponent);
		if (references == null) {
			references = apiComponent.getExternalDependencies();
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, SearchMessages.collecting_external_dependencies, 10);
		try {
			ArrayList unavailableMembers = new ArrayList();
			if (apiUseTypes != null && apiUseTypes.length > 0) {
				for (int i = 0; i < apiUseTypes.length; i++) {
					if (!references.hasReferencesTo(apiUseTypes[i])) {
						unavailableMembers.add(apiUseTypes[i]);
					}
				}
				if (unavailableMembers.size() > 0) {
					fetch(apiComponent, (String[]) unavailableMembers.toArray(new String[unavailableMembers.size()]), references, monitor);
				}
				Util.updateMonitor(localmonitor, 1);
				return references.getExternalDependenciesTo(apiUseTypes);
			} else {
				fetch(apiComponent, null, references, localmonitor.newChild(8)); // full build has been triggered so re-fetch
				Util.updateMonitor(localmonitor, 1);
				return references.getAllExternalDependencies();
			}
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * fetches the references from the API Use Scan locations
	 * @param apiComponent
	 * @param member
	 * @param references
	 * @param monitor
	 */
	private void fetch(IApiComponent apiComponent, String[] types, IReferenceCollection references, IProgressMonitor monitor) {
		UseScanParser parser = new UseScanParser();
		UseScanReferenceVisitor visitor = new UseScanReferenceVisitor(apiComponent, types, references);
		SubMonitor localmonitor = SubMonitor.convert(monitor, SearchMessages.load_external_dependencies, 10);
		try {
			String[] locations;
			if (fLocations == null) {
				locations = getReportLocations();
			}
			else {
				locations = fLocations;
			}
			if (locations != null) {
				IStringVariableManager stringManager = null;
				localmonitor.setWorkRemaining(locations.length * 2);
				for (int i = 0; i < locations.length; i++) {
					Util.updateMonitor(localmonitor, 1);
					File file = new File(locations[i]);
					if (!file.exists()) {
						continue;
					}
					if (file.isFile()) {
						if (Util.isArchive(file.getName())) {
							String destDirPath = tempLocation + file.getName() + '.' + file.getAbsolutePath().hashCode();
							if (stringManager == null) {
								stringManager = VariablesPlugin.getDefault().getStringVariableManager();
							}
							destDirPath = stringManager.performStringSubstitution(destDirPath);
							locations[i] = destDirPath + '/' + file.lastModified();
							File unzipDirLoc = new File(destDirPath);
							if (unzipDirLoc.exists()) {
								String[] childDirs = unzipDirLoc.list();
								for (int j = 0; j < childDirs.length; j++) {
									if (!childDirs[j].equals(String.valueOf(file.lastModified()))) {
										FileManager.getManager().recordTempFileRoot(destDirPath + '/' + childDirs[j]);
									}
								}
							} else {
								Util.unzip(file.getPath(), locations[i]);
							}							
						} else {
							continue;
						}
					}
					try {
						locations[i] = getExactScanLocation(locations[i]);
						if (locations[i] == null) {
							String message;
							if (file.isDirectory()) {
								message = NLS.bind(SearchMessages.UseScanManager_InvalidDir, file.getAbsolutePath());
							} else {
								message = NLS.bind(SearchMessages.UseScanManager_InvalidArchive, file.getAbsolutePath());
							}
							throw new Exception(message);
						}
						parser.parse(locations[i], localmonitor.newChild(2), visitor);
						Util.updateMonitor(localmonitor);
					} catch (Exception e) {
						ApiPlugin.log(e); // log the exception and continue with next location
					}
				}
				fApiComponentCache.remove(apiComponent); // remove current value so that it only doesn't gets purged if size limit is reached
				fApiComponentCache.put(apiComponent, references);
			}
		} catch (Exception e) {
			ApiPlugin.log(e);
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns the scan 
	 * @param location
	 * @return
	 */
	public static String getExactScanLocation(String location) {
		File file = new File(location);
		if (isValidDirectory(file)) {
			return location;
		}
		file = new File(location, IApiCoreConstants.XML);
		if (isValidDirectory(file)) {
			return file.getAbsolutePath();
		}
		return null;
	}
	
	/**
	 * Validate if the given {@link File} is a folder that contains a use scan.
	 * <br><br> 
	 * The {@link File} is considered valid iff:
	 * <ul>
	 * <li>it is a folder</li>
	 * <li>the folder has child folder that matches the name pattern <code>^.* (.*)$</code></li>
	 * <li>the previous child directory has its own child directory that matches the name pattern <code>^.* (.*)$</code></li>
	 * </ul>
	 * @param file
	 * @return <code>true</code> is the sub folders match the patterns, <code>false</code> otherwise
	 */
	public static boolean isValidDirectory(File file) {
		if(file.exists() && file.isDirectory()) {
			try {
				file.listFiles(USESCAN_FILTER);
			}
			catch(RuntimeException rte) {
				File f = new File(file, rte.getMessage());
				try {
					if(f.exists() && f.isDirectory()) {
						f.listFiles(USESCAN_FILTER);
					}
				}
				catch(RuntimeException re) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Validate if the given {@link File} is an archive that contains a use scan.
	 * <br><br> 
	 * The {@link File} is considered valid iff:
	 * <ul>
	 * <li>it has an XML folder</li>
	 * <li>the XML folder has child folder that matches the name pattern <code>{@link #NAME_REGEX}</code></li>
	 * <li>the previous child directory has its own child directory that matches the name pattern <code>{@link #NAME_REGEX}</code></li>
	 * </ul>
	 * @param file
	 * @return <code>true</code> is the sub folders match the patterns, <code>false</code> otherwise
	 */
	public static boolean isValidArchive(File file) {
		String fname = file.getName().toLowerCase();
		if(file.exists() && Util.isArchive(fname)) {
			Enumeration entries = null;
			if(fname.endsWith(Util.DOT_JAR)) {
				try {
					JarFile jfile = new JarFile(file);
					entries = jfile.entries();
				}
				catch(IOException ioe) {
					return false;
				}
			}
			else if(fname.endsWith(Util.DOT_ZIP)) {
				try {
					ZipFile zfile = new ZipFile(file);
					entries = zfile.entries();
				} catch (IOException e) {
					return false;
				}
			}
			if(entries != null) {
				while(entries.hasMoreElements()) {
					ZipEntry o = (ZipEntry) entries.nextElement();
					if(o.isDirectory()) {
						IPath path = new Path(o.getName());
						int count = path.segmentCount();
						if(count > 2) {
							return NAME_REGEX.matcher(path.segment(0)).matches() || NAME_REGEX.matcher(path.segment(1)).matches();
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Returns if the scan if a valid API use scan
	 * @param location
	 * @return true if the scan is valid false otherwise
	 */
	public static boolean isValidScanLocation(String location) {
		if (location != null && location.length() > 0) {
			IPath path = new Path(location);		
			File file = path.toFile();
			return isValidDirectory(file) || isValidArchive(file);
		}
		return false;
	}
	
	/**
	 * Returns the report locations from the preferences
	 * @return
	 */
	public String[] getReportLocations() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		String apiUseScanPaths = node.get(IApiCoreConstants.API_USE_SCAN_LOCATION, null);
		if (apiUseScanPaths == null || apiUseScanPaths.length() == 0) {
			return new String[0];
		}
		
		String[] locations = apiUseScanPaths.split(ESCAPE_REGEX + LOCATION_DELIM);
		ArrayList locationList = new ArrayList(locations.length);
		for (int i = 0; i < locations.length; i++) {
			String values[] = locations[i].split(ESCAPE_REGEX + STATE_DELIM);
			if (Boolean.valueOf(values[1]).booleanValue())
				locationList.add(values[0]);
		}
		return (String[]) locationList.toArray(new String[locationList.size()]);
	}

	/**
	 * Sets the report locations to be used. Once set, these locations will be used instead of ones in the preference.
	 * When set to <code>null</code>, the locations in preference will be used.
	 * @param locations
	 */
	public void setReportLocations(String[] locations) {
		fLocations = locations;
	}

	/**
	 * Sets the cache size
	 * @param size The total number of references that can be held in memory
	 */
	public void setCacheSize(int size) {
		fApiComponentCache.setSpaceLimit(size);
	}

	/**
	 * Purges all reference information
	 */
	public void clearCache() {
		Enumeration elementss = fApiComponentCache.elements();
		while (elementss.hasMoreElements()) {
			IReferenceCollection reference = (IReferenceCollection) elementss.nextElement();
			reference.clear();
		}
		fApiComponentCache.flush();
	}
}
