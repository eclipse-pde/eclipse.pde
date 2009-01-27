/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Utils;
import org.osgi.framework.Bundle;

public class ProfileManager {
	public static final String PROFILE_EXTENSION = ".profile"; //$NON-NLS-1$
	public static final String SYSTEM_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$
	public static final String PROFILE_NAME = "osgi.java.profile.name"; //$NON-NLS-1$
	public static final String PROFILE_LIST = "profile.list"; //$NON-NLS-1$
	public static final String JAVA_PROFILES = "java.profiles"; //$NON-NLS-1$
	public static final String PROFILE_JAVAC_SOURCE = "org.eclipse.jdt.core.compiler.source"; //$NON-NLS-1$
	public static final String PROFILE_JAVAC_TARGET = "org.eclipse.jdt.core.compiler.codegen.targetPlatform"; //$NON-NLS-1$

	private final HashMap profileMap = new HashMap();
	private String[] profileSources = null;

	public ProfileManager() {
		loadProfiles(true);
	}

	public ProfileManager(String[] profileSources) {
		this.profileSources = profileSources;
		loadProfiles(false);
	}

	public ProfileManager(String[] profileSources, boolean includeRuntime) {
		this.profileSources = profileSources;
		loadProfiles(includeRuntime);
	}

	public Properties getProfileProperties(String profileName) {
		return (Properties) profileMap.get(profileName);
	}

	public void copyEEProfileProperties(Dictionary source, Properties target) {
		String[] profiles = getJavaProfiles();
		for (int i = 0; i < profiles.length; i++) {
			Object value = source.get(profiles[i]);
			if (value != null && value instanceof String) {
				target.put(profiles[i], value);
			}
		}
	}

	/**
	 * Return the javacSource to use when compiling for the given execution environment
	 * @param environment
	 * @return javacSource or null
	 */
	public String getJavacSource(String environment) {
		if (profileMap.containsKey(environment)) {
			Properties properties = (Properties) profileMap.get(environment);
			return properties.getProperty(PROFILE_JAVAC_SOURCE);
		}
		return null;
	}

	/**
	 * Return the javacTarget to use when compiling for the given execution environment
	 * @param environment
	 * @return javacTarget or null
	 */
	public String getJavacTarget(String environment) {
		if (profileMap.containsKey(environment)) {
			Properties properties = (Properties) profileMap.get(environment);
			return properties.getProperty(PROFILE_JAVAC_TARGET);
		}
		return null;
	}

	public String[] getJavaProfiles() {
		Set keys = profileMap.keySet();
		return sortProfiles((String[]) keys.toArray(new String[keys.size()]));
	}

	protected String[] sortProfiles(String[] profiles) {
		Arrays.sort(profiles, new Comparator() {
			public int compare(Object profile1, Object profile2) {
				// need to make sure JavaSE, J2SE profiles are sorted ahead of all other profiles
				String p1 = (String) profile1;
				String p2 = (String) profile2;
				if (p1.startsWith("JavaSE") && !p2.startsWith("JavaSE")) //$NON-NLS-1$ //$NON-NLS-2$
					return -1;
				if (!p1.startsWith("JavaSE") && p2.startsWith("JavaSE")) //$NON-NLS-1$ //$NON-NLS-2$
					return 1;
				if (p1.startsWith("J2SE") && !p2.startsWith("J2SE")) //$NON-NLS-1$ //$NON-NLS-2$
					return -1;
				if (!p1.startsWith("J2SE") && p2.startsWith("J2SE")) //$NON-NLS-1$ //$NON-NLS-2$
					return 1;
				return -p1.compareTo(p2);
			}
		});
		return profiles;
	}

	protected void loadProfiles(boolean includeRuntime) {
		if (includeRuntime || profileSources == null) {
			loadRuntimeJavaProfiles();
		}
		if (profileSources != null) {
			for (int i = 0; i < profileSources.length; i++) {
				File source = new File(profileSources[i]);
				if (source.isDirectory()) {
					loadJavaProfiles(source);
				} else {
					loadJarJavaProfiles(source);
				}
			}
		}
	}

	protected void loadJarJavaProfiles(File bundleLocation) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
			loadJavaProfiles(zipFile);
		} catch (IOException e) {
			// boo
		} finally {
			Utils.close(zipFile);
		}
	}

	protected void loadRuntimeJavaProfiles() {
		Bundle systemBundle = Platform.getBundle(IPDEBuildConstants.BUNDLE_OSGI);
		if (systemBundle != null)
			loadJavaProfiles(systemBundle);
	}

	/*
	 * Return an input stream on the profile.list entry in the given container.
	 */
	private InputStream getProfileListInputStream(Object container) {
		if (container instanceof File) {
			// try the profile list first
			File listFile = new File((File) container, PROFILE_LIST);
			if (listFile.exists())
				try {
					return new BufferedInputStream(new FileInputStream(listFile));
				} catch (FileNotFoundException e) {
					return null;
				}
		} else if (container instanceof ZipFile) {
			ZipFile zipFile = (ZipFile) container;
			ZipEntry listEntry = ((ZipFile) container).getEntry(PROFILE_LIST);
			if (listEntry != null)
				try {
					return new BufferedInputStream(zipFile.getInputStream(listEntry));
				} catch (IOException e) {
					return null;
				}
		} else if (container instanceof Bundle) {
			Bundle systemBundle = (Bundle) container;
			URL url = systemBundle.getEntry(PROFILE_LIST);
			if (url != null) {
				try {
					return new BufferedInputStream(url.openStream());
				} catch (IOException e) {
					return null;
				}
			}
		}
		return null;
	}

	/*
	 * Return an Enumeration containing all the profile entries in the container
	 * If profiles is null then:
	 *   A ZipFile container returns all ZipEntries from the jar
	 *   All other containers return the *.profile entries from the root  
	 */
	private Enumeration getProfilesEnum(Object container, String[] profiles) {
		if (profiles != null) {
			return Utils.getArrayEnumerator(profiles);
		} else if (container instanceof File) {
			File[] files = ((File) container).listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(PROFILE_EXTENSION);
				}
			});
			return Utils.getArrayEnumerator(files);
		} else if (container instanceof ZipFile) {
			return ((ZipFile) container).entries();
		} else if (container instanceof Bundle) {
			return ((Bundle) container).findEntries("/", "*" + PROFILE_EXTENSION, false); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	private InputStream getEntryInputStream(Object container, Object entry) {
		try {
			if (entry instanceof String) {
				if (container instanceof File)
					entry = new File((File) container, (String) entry);
				else if (container instanceof ZipFile)
					entry = ((ZipFile) container).getEntry((String) entry);
				else if (container instanceof Bundle)
					entry = ((Bundle) container).getEntry((String) entry);
			}

			if (entry instanceof File)
				return new BufferedInputStream(new FileInputStream((File) entry));
			else if (entry instanceof ZipEntry)
				return new BufferedInputStream(((ZipFile) container).getInputStream((ZipEntry) entry));
			else if (entry instanceof URL)
				return new BufferedInputStream(((URL) entry).openStream());
		} catch (IOException e) {
			// boo
		}
		return null;
	}

	/*
	 * ZipFile conainers aren't able to filter the entry enumeration, must filter here.
	 */
	private boolean isProfileEntry(Object entry) {
		if (entry instanceof String || entry instanceof URL || entry instanceof File)
			return true;
		else if (entry instanceof ZipEntry) {
			String entryName = ((ZipEntry) entry).getName();
			return entryName.indexOf('/') < 0 && entryName.endsWith(PROFILE_EXTENSION);
		}
		return false;
	}

	/**
	 * Load the Java Profiles from the given container.  A container is one of:
	 *     File - a directory shaped bundle on disk
	 *     ZipFile - a jar shaped bundle on disk
	 *     Bundle - a Bundle object from the current runtime.
	 * @param container : The container to load java profiles from. 
	 */
	private void loadJavaProfiles(Object container) {
		InputStream is = getProfileListInputStream(container);
		String[] profiles = getJavaProfiles(is);
		Utils.close(is);

		Enumeration entries = getProfilesEnum(container, profiles);
		while (entries != null && entries.hasMoreElements()) {
			Object item = entries.nextElement();
			if (!isProfileEntry(item))
				continue;

			is = getEntryInputStream(container, item);
			if (is != null) {
				Properties props = new Properties();
				try {
					props.load(is);
					if (props.containsKey(PROFILE_NAME))
						profileMap.put(props.get(PROFILE_NAME), props);
				} catch (IOException e) {
					//boo
				} finally {
					Utils.close(is);
				}
			}
		}
	}

	private String[] getJavaProfiles(InputStream is) {
		if (is == null)
			return null;
		Properties props = new Properties();
		try {
			props.load(is);
		} catch (IOException e) {
			return null;
		}
		return ManifestElement.getArrayFromList(props.getProperty(JAVA_PROFILES), ","); //$NON-NLS-1$
	}

}
