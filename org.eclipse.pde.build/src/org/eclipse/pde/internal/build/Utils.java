/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * General utility class.
 */
public final class Utils implements IPDEBuildConstants, IBuildPropertiesConstants, IXMLConstants {
	static class ArrayEnumeration implements Enumeration {
		private final Object[] array;
		int cur = 0;

		public ArrayEnumeration(Object[] array) {
			this.array = new Object[array.length];
			System.arraycopy(array, 0, this.array, 0, this.array.length);
		}

		public boolean hasMoreElements() {
			return cur < array.length;
		}

		public Object nextElement() {
			return array[cur++];
		}
	}

	// The 64 characters that are legal in a version qualifier, in lexicographical order.
	private static final String BASE_64_ENCODING = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"; //$NON-NLS-1$

	// regex expressions and keys for parsing feature root properties
	private static final String REGEX_ROOT_CONFIG = "^root((\\.[\\w-\\*]+){3})$"; //$NON-NLS-1$
	private static final String REGEX_ROOT_CONFIG_FOLDER = "^root((\\.[\\w-\\*]+){3})?\\.folder\\.(.*)$"; //$NON-NLS-1$
	private static final String REGEX_ROOT_CONFIG_PERMISSIONS = "^root((\\.[\\w-\\*]+){3})?\\.permissions\\.(.*)$"; //$NON-NLS-1$
	private static final String REGEX_ROOT_CONFIG_LINK = "^root((\\.[\\w-\\*]+){3})?\\.link$"; //$NON-NLS-1$
	public static final String ROOT_PERMISSIONS = "!!ROOT.PERMISSIONS!!"; //$NON-NLS-1$
	public static final String ROOT_LINK = "!!ROOT.LINK!!"; //$NON-NLS-1$
	public static final String ROOT_COMMON = "!!COMMON!!"; //$NON-NLS-1$

	/** 
	 * returns a value 1 - 64 for valid qualifier characters.  Returns 0 for non-valid characters 
	 */
	public static int qualifierCharValue(char c) {
		int index = BASE_64_ENCODING.indexOf(c);
		// The "+ 1" is very intentional.  For a blank (or anything else that
		// is not a legal character), we want to return 0.  For legal
		// characters, we want to return one greater than their position, so
		// that a blank is correctly distinguished from '-'.
		return index + 1;
	}

	// Integer to character conversion in our base-64 encoding scheme.  If the
	// input is out of range, an illegal character will be returned.
	public static char base64Character(int number) {
		if (number < 0 || number > 63) {
			return ' ';
		}
		return BASE_64_ENCODING.charAt(number);
	}

	public static VersionRange createVersionRange(String versionId) {
		VersionRange range = null;
		if (versionId == null || versionId.length() == 0 || GENERIC_VERSION_NUMBER.equals(versionId))
			range = VersionRange.emptyRange;
		else {
			int qualifierIdx = versionId.indexOf(IBuildPropertiesConstants.PROPERTY_QUALIFIER);
			if (qualifierIdx != -1) {
				String newVersion = versionId.substring(0, qualifierIdx);
				if (newVersion.endsWith(".")) //$NON-NLS-1$
					newVersion = newVersion.substring(0, newVersion.length() - 1);

				Version lower = new Version(newVersion);
				Version upper = null;
				String newQualifier = incrementQualifier(lower.getQualifier());
				if (newQualifier == null)
					upper = new Version(lower.getMajor(), lower.getMinor(), lower.getMicro() + 1);
				else
					upper = new Version(lower.getMajor(), lower.getMinor(), lower.getMicro(), newQualifier);
				range = new VersionRange(lower, true, upper, false);
			} else {
				range = new VersionRange(new Version(versionId), true, new Version(versionId), true);
			}
		}
		return range;
	}

	public static VersionRange createVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null)
			return VersionRange.emptyRange;

		Version version = new Version(versionSpec);
		if (version.equals(Version.emptyVersion))
			return VersionRange.emptyRange;

		String match = entry.getMatch();
		if (!entry.isRequires() || match == null) {
			return createVersionRange(versionSpec);
		}

		if (match.equals("perfect")) //$NON-NLS-1$
			return new VersionRange(version, true, version, true);
		if (match.equals("equivalent")) { //$NON-NLS-1$
			Version upper = new Version(version.getMajor(), version.getMinor() + 1, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("compatible")) { //$NON-NLS-1$
			Version upper = new Version(version.getMajor() + 1, 0, 0);
			return new VersionRange(version, true, upper, false);
		}
		if (match.equals("greaterOrEqual")) //$NON-NLS-1$
			return new VersionRange(version, true, new VersionRange(null).getMaximum(), true);

		return VersionRange.emptyRange;
	}

	private static String incrementQualifier(String qualifier) {
		int idx = qualifier.length() - 1;

		for (; idx >= 0; idx--) {
			//finding last non-'z' character
			if (qualifier.charAt(idx) != 'z')
				break;
		}

		if (idx >= 0) {
			// qualifierCharValue returns 1 - 64, this is an implicit +1 over
			// the characters returned by base64Character
			int c = Utils.qualifierCharValue(qualifier.charAt(idx));
			String newQualifier = qualifier.substring(0, idx);
			newQualifier += Utils.base64Character(c);
			return newQualifier;
		}

		return null;
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified. The spcecificity of this method is that it returns an empty
	 * element when to same separators are following each others. For example
	 * the string a,,b returns the following array [a, ,b]
	 *  
	 */
	public static String[] getArrayFromStringWithBlank(String list, String separator) {
		if (list == null || list.trim().length() == 0)
			return new String[0];
		List result = new ArrayList();
		boolean previousWasSeparator = true;
		for (StringTokenizer tokens = new StringTokenizer(list, separator, true); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (token.equals(separator)) {
				if (previousWasSeparator)
					result.add(""); //$NON-NLS-1$
				previousWasSeparator = true;
			} else {
				result.add(token);
				previousWasSeparator = false;
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Return a string array constructed from the given list of comma-separated
	 * tokens.
	 * 
	 * @param list
	 *            the list to convert
	 * @return the array of strings
	 */
	public static String[] getArrayFromString(String list) {
		return getArrayFromString(list, ","); //$NON-NLS-1$
	}

	/**
	 * Converts an array of strings into an array of URLs.
	 * 
	 * @param target
	 * @return URL[]
	 * @throws CoreException
	 */
	public static URL[] asURL(String[] target) throws CoreException {
		if (target == null)
			return null;
		try {
			URL[] result = new URL[target.length];
			for (int i = 0; i < target.length; i++)
				result[i] = new URL(target[i]);
			return result;
		} catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, e.getMessage(), e));
		}
	}

	public static URL[] asURL(Collection target) throws CoreException {
		if (target == null)
			return null;
		try {
			URL[] result = new URL[target.size()];
			int i = 0;
			for (Iterator iter = target.iterator(); iter.hasNext();) {
				result[i++] = ((File) iter.next()).toURL();
			}
			return result;
		} catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_MALFORMED_URL, e.getMessage(), e));
		}
	}

	public static File[] asFile(String[] target) {
		if (target == null)
			return new File[0];
		File[] result = new File[target.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new File(target[i]);
		}
		return result;
	}

	public static File[] asFile(URL[] target) {
		if (target == null)
			return new File[0];
		File[] result = new File[target.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new File(target[i].getFile());
		}
		return result;
	}

	public static File[] asFile(Collection collection) {
		if (collection.size() == 0)
			return new File[0];
		Object first = collection.iterator().next();
		if (first instanceof String)
			return asFile((String[]) collection.toArray(new String[collection.size()]));
		else if (first instanceof URL)
			return asFile((URL[]) collection.toArray(new URL[collection.size()]));
		else if (first instanceof File)
			return (File[]) collection.toArray(new File[collection.size()]);
		throw new IllegalArgumentException();
	}

	/**
	 * Return a string which is a concatination of each member of the given
	 * collection, separated by the given separator.
	 * 
	 * @param collection
	 *            the collection to concatinate
	 * @param separator
	 *            the separator to use
	 * @return String
	 */
	public static String getStringFromCollection(Collection collection, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (Iterator i = collection.iterator(); i.hasNext();) {
			if (first)
				first = false;
			else
				result.append(separator);
			result.append(i.next());
		}
		return result.toString();
	}

	/**
	 * Return a string which is a concatination of each member of the given
	 * array, separated by the given separator.
	 * 
	 * @param values
	 *            the array to concatinate
	 * @param separator
	 *            the separator to use
	 * @return String
	 */
	public static String getStringFromArray(String[] values, String separator) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				if (i > 0)
					result.append(separator);
				result.append(values[i]);
			}
		}
		return result.toString();
	}

	/**
	 * Return a path which is equivalent to the given location relative to the
	 * specified base path.
	 * 
	 * @param location
	 *            the location to convert
	 * @param base
	 *            the base path
	 * @return IPath
	 */
	public static IPath makeRelative(IPath location, IPath base) {
		//can't make relative if the devices don't match
		if (location.getDevice() == null) {
			if (base.getDevice() != null)
				return location;
		} else {
			if (!location.getDevice().equalsIgnoreCase(base.getDevice()))
				return location;
		}
		int baseCount = base.segmentCount();
		int count = base.matchingFirstSegments(location);
		String temp = ""; //$NON-NLS-1$
		for (int j = 0; j < baseCount - count; j++)
			temp += "../"; //$NON-NLS-1$
		return new Path(temp).append(location.removeFirstSegments(count));
	}

	/**
	 * Transfers all available bytes from the given input stream to the given
	 * output stream. Regardless of failure, this method closes both streams.
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	static public void copyFile(String src, String dest) throws IOException {
		File source = new File(src);
		if (!source.exists())
			return;
		File destination = new File(dest);
		File destDir = destination.getParentFile();
		if ((!destDir.exists() && !destDir.mkdirs()) || destDir.isFile())
			return; //we will fail trying to create the file, TODO log warning/error

		copy(source, destination);
	}

	public static void copy(File source, File destination) throws IOException {
		org.eclipse.pde.internal.publishing.Utils.copy(source, destination);
	}

	public static void writeBuffer(StringBuffer buffer, File outputFile) throws IOException {
		FileOutputStream stream = null;
		try {
			outputFile.getParentFile().mkdirs();
			stream = new FileOutputStream(outputFile);
			stream.write(buffer.toString().getBytes());
		} finally {
			close(stream);
		}
	}

	public static void writeProperties(Properties properites, File outputFile, String comment) throws IOException {
		outputFile.getParentFile().mkdirs();
		OutputStream buildFile = new BufferedOutputStream(new FileOutputStream(outputFile));
		try {
			properites.store(buildFile, comment);
		} finally {
			close(buildFile);
		}
	}

	public static FeatureEntry[] getPluginEntry(BuildTimeFeature feature, String pluginId, boolean raw) {
		FeatureEntry[] plugins;
		if (raw)
			plugins = feature.getRawPluginEntries();
		else
			plugins = feature.getPluginEntries();
		List foundEntries = new ArrayList(5);

		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].getId().equals(pluginId))
				foundEntries.add(plugins[i]);
		}
		return (FeatureEntry[]) foundEntries.toArray(new FeatureEntry[foundEntries.size()]);

	}

	// Return a collection of File, the result can be null
	public static Collection findFiles(File from, String foldername, final String filename) {
		// if from is a file which name match filename, then simply return the
		// file
		File root = from;
		if (root.isFile() && root.getName().equals(filename)) {
			Collection coll = new ArrayList(1);
			coll.add(root);
			return coll;
		}

		Collection collectedElements = new ArrayList(10);

		File[] featureDirectoryContent = new File(from, foldername).listFiles();
		if (featureDirectoryContent == null)
			return null;

		for (int i = 0; i < featureDirectoryContent.length; i++) {
			if (featureDirectoryContent[i].isDirectory()) {
				File[] featureFiles = featureDirectoryContent[i].listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.equals(filename);
					}
				});
				if (featureFiles.length != 0)
					collectedElements.add(featureFiles[0]);
			}
		}
		return collectedElements;
	}

	public static boolean isIn(FeatureEntry[] array, FeatureEntry element) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].getId().equals(element.getId()) && array[i].getVersion().equals(element.getVersion()))
				return true;
		}
		return false;
	}

	public static Collection copyFiles(String fromDir, String toDir) throws CoreException {
		File templateLocation = new File(fromDir);
		Collection copiedFiles = new ArrayList();
		if (templateLocation.exists()) {
			File[] files = templateLocation.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						File subDir = new File(toDir, files[i].getName());
						if (!subDir.exists())
							subDir.mkdirs();
						Collection subFiles = copyFiles(fromDir + '/' + files[i].getName(), toDir + '/' + files[i].getName());
						for (Iterator iter = subFiles.iterator(); iter.hasNext();) {
							String sub = (String) iter.next();
							copiedFiles.add(files[i].getName() + '/' + sub);
						}
						continue;
					}

					FileInputStream inputStream = null;
					FileOutputStream outputStream = null;

					try {
						inputStream = new FileInputStream(files[i]);
					} catch (FileNotFoundException e) {
						String message = NLS.bind(Messages.exception_missingFile, files[i].getAbsolutePath());
						throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
					}

					String fileToCopy = toDir + '/' + files[i].getName();
					try {
						outputStream = new FileOutputStream(fileToCopy);
					} catch (FileNotFoundException e) {
						String message = NLS.bind(Messages.exception_missingFile, fileToCopy);
						throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
					}

					try {
						Utils.transferStreams(inputStream, outputStream);
						copiedFiles.add(files[i].getName());
					} catch (IOException e) {
						String message = NLS.bind(Messages.exception_writingFile, fileToCopy);
						throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
					}
				}
			}
		}
		return copiedFiles;
	}

	public static List extractPlugins(List initialList, Set toExtract) {
		//TODO This algorithm needs to be  improved
		if (initialList.size() == toExtract.size())
			return initialList;
		List result = new ArrayList(toExtract.size());
		for (Iterator iter = initialList.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (toExtract.contains(element)) {
				result.add(element);
				if (result.size() == toExtract.size())
					break;
			}
		}
		return result;
	}

	public static int isStringIn(String[] searched, String toSearch) {
		if (searched == null || toSearch == null)
			return -1;
		for (int i = 0; i < searched.length; i++) {
			if (toSearch.startsWith(searched[i]))
				return i;
		}
		return -1;
	}

	static public Properties getOldExecutableRootOverrides() {
		Properties overrides = new Properties();
		overrides.put("root.win32.win32.x86", "file:bin/win32/win32/x86/launcher.exe"); //$NON-NLS-1$ //$NON-NLS-2$
		overrides.put("root.win32.win32.x86_64", "file:bin/win32/win32/x86_64/launcher.exe"); //$NON-NLS-1$ //$NON-NLS-2$
		overrides.put("root.win32.wpf.x86", "file:bin/wpf/win32/x86/launcher.exe"); //$NON-NLS-1$ //$NON-NLS-2$
		return overrides;
	}

	/**
	 * Process root file properties.  
	 * Resulting map is from config string to a property map.  The format of the property map is:
	 * 1) folder -> fileset to copy.  folder can be "" (the root) or an actual folder
	 * 2) ROOT_PERMISSIONS + rights -> fileset to set rights for
	 * 3) ROOT_LINK -> comma separated list: (target, link)*
	 * 
	 * Properties that are common across all configs are available under the ROOT_COMMON key.
	 * They are also optionally merged into each individual config.

	 * @param properties - build.properties for a feature
	 * @param mergeCommon - whether or not to merge the common properties into each config
	 * @return Map
	 */
	static public Map processRootProperties(Properties properties, boolean mergeCommon) {
		Map map = new HashMap();
		Map common = new HashMap();
		for (Enumeration keys = properties.keys(); keys.hasMoreElements();) {
			String entry = (String) keys.nextElement();
			String config = null;
			String entryKey = null;

			if (entry.equals(ROOT) || entry.matches(REGEX_ROOT_CONFIG)) {
				config = entry.length() > 4 ? entry.substring(5) : ""; //$NON-NLS-1$
				entryKey = ""; //$NON-NLS-1$
			} else if (entry.matches(REGEX_ROOT_CONFIG_FOLDER)) {
				int folderIdx = entry.indexOf(FOLDER_INFIX);
				config = (folderIdx > 5) ? entry.substring(5, folderIdx) : ""; //$NON-NLS-1$
				entryKey = entry.substring(folderIdx + 8);
			} else if (entry.matches(REGEX_ROOT_CONFIG_PERMISSIONS)) {
				int permissionIdx = entry.indexOf(PERMISSIONS_INFIX);
				config = (permissionIdx > 5) ? entry.substring(5, permissionIdx) : ""; //$NON-NLS-1$
				entryKey = ROOT_PERMISSIONS + entry.substring(permissionIdx + 13);
			} else if (entry.matches(REGEX_ROOT_CONFIG_LINK)) {
				int linkIdx = entry.indexOf(LINK_SUFFIX);
				config = (linkIdx > 5) ? entry.substring(5, linkIdx) : ""; //$NON-NLS-1$
				entryKey = ROOT_LINK;
			}

			if (config != null) {
				Map submap = (config.length() == 0) ? common : (Map) map.get(config);
				if (submap == null) {
					submap = new HashMap();
					map.put(config, submap);
				}
				if (submap.containsKey(entryKey)) {
					String existing = (String) submap.get(entryKey);
					submap.put(entryKey, existing + "," + properties.getProperty(entry)); //$NON-NLS-1$
				} else {
					submap.put(entryKey, properties.get(entry));
				}
			}
		}

		//merge the common properties into each of the configs
		if (common.size() > 0 && mergeCommon) {
			for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				Map submap = (Map) map.get(key);
				for (Iterator commonKeys = common.keySet().iterator(); commonKeys.hasNext();) {
					String commonKey = (String) commonKeys.next();
					if (submap.containsKey(commonKey)) {
						String existing = (String) submap.get(commonKey);
						submap.put(commonKey, existing + "," + common.get(commonKey)); //$NON-NLS-1$
					} else {
						submap.put(commonKey, common.get(commonKey));
					}
				}
			}
		}

		//and also add the common properties independently
		if (mergeCommon || common.size() > 0)
			map.put(ROOT_COMMON, common);
		return map;
	}

	public static void generatePermissions(Properties featureProperties, Config aConfig, String targetRootProperty, AntScript script) {
		if (featureProperties == null)
			return;
		String configInfix = aConfig.toString("."); //$NON-NLS-1$
		String configPath = aConfig.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
		String prefixPermissions = ROOT_PREFIX + configInfix + '.' + PERMISSIONS + '.';
		String prefixLinks = ROOT_PREFIX + configInfix + '.' + LINK;
		String commonPermissions = ROOT_PREFIX + PERMISSIONS + '.';
		String commonLinks = ROOT_PREFIX + LINK;
		for (Iterator iter = featureProperties.entrySet().iterator(); iter.hasNext();) {
			Map.Entry permission = (Map.Entry) iter.next();
			String instruction = (String) permission.getKey();
			String parameters = removeEndingSlashes((String) permission.getValue());
			if (instruction.startsWith(prefixPermissions)) {
				generateChmodInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configPath + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), instruction.substring(prefixPermissions.length()), parameters);
				continue;
			}
			if (instruction.startsWith(prefixLinks)) {
				generateLinkInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configPath + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), parameters);
				continue;
			}
			if (instruction.startsWith(commonPermissions)) {
				generateChmodInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configPath + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), instruction.substring(commonPermissions.length()), parameters);
				continue;
			}
			if (instruction.startsWith(commonLinks)) {
				generateLinkInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configPath + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), parameters);
				continue;
			}
		}
	}

	public static String removeEndingSlashes(String value) {
		String[] params = Utils.getArrayFromString(value, ","); //$NON-NLS-1$
		for (int i = 0; i < params.length; i++) {
			if (params[i].endsWith("/")) //$NON-NLS-1$
				params[i] = params[i].substring(0, params[i].length() - 1);
		}
		return Utils.getStringFromArray(params, ","); //$NON-NLS-1$
	}

	private static void generateChmodInstruction(AntScript script, String dir, String rights, String files) {
		if (rights.equals(EXECUTABLE)) {
			rights = "755"; //$NON-NLS-1$
		}
		script.printChmod(dir, rights, files);
	}

	private static void generateLinkInstruction(AntScript script, String dir, String files) {
		String[] links = Utils.getArrayFromString(files, ","); //$NON-NLS-1$
		List arguments = new ArrayList(2);
		for (int i = 0; i < links.length; i += 2) {
			arguments.add("-sf"); //$NON-NLS-1$
			arguments.add(links[i]);
			arguments.add(links[i + 1]);
			script.printExecTask("ln", dir, arguments, "Linux"); //$NON-NLS-1$ //$NON-NLS-2$
			arguments.clear();
		}
	}

	/**
	 * Return a string with the given property name in the format:
	 * <pre>${propertyName}</pre>.
	 * 
	 * @param propertyName the name of the property
	 * @return String
	 */
	public static String getPropertyFormat(String propertyName) {
		StringBuffer sb = new StringBuffer();
		sb.append(PROPERTY_ASSIGNMENT_PREFIX);
		sb.append(propertyName);
		sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
		return sb.toString();
	}

	public static String getMacroFormat(String propertyName) {
		StringBuffer sb = new StringBuffer();
		sb.append(MACRO_ASSIGNMENT_PREFIX);
		sb.append(propertyName);
		sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
		return sb.toString();
	}

	public static boolean isBinary(BundleDescription bundle) {
		Properties bundleProperties = ((Properties) bundle.getUserObject());
		if (bundleProperties == null || bundleProperties.get(IS_COMPILED) == null) {
			File props = new File(bundle.getLocation(), PROPERTIES_FILE);
			return !(props.exists() && props.isFile());
		}
		return (Boolean.FALSE == bundleProperties.get(IS_COMPILED));
	}

	public static boolean isSourceBundle(BundleDescription bundle) {
		Properties bundleProperties = (Properties) bundle.getUserObject();
		return (bundleProperties != null && bundleProperties.containsKey(ECLIPSE_SOURCE_BUNDLE));
	}

	public static boolean hasBundleShapeHeader(BundleDescription bundle) {
		Properties bundleProperties = (Properties) bundle.getUserObject();
		return (bundleProperties != null && bundleProperties.containsKey(ECLIPSE_BUNDLE_SHAPE));
	}

	public static String getSourceBundleHeader(BundleDescription bundle) {
		Properties bundleProperties = (Properties) bundle.getUserObject();
		if (bundleProperties == null || !bundleProperties.containsKey(ECLIPSE_SOURCE_BUNDLE))
			return ""; //$NON-NLS-1$

		String header = bundleProperties.getProperty(ECLIPSE_SOURCE_BUNDLE);
		return header;
	}

	/**
	 * Given a newly generated old-style source bundle for which there was a previously existing
	 * version in the target, return the location of the src folder in that earlier version
	 * @param bundle
	 * @return the old version's src folder, or null
	 */
	public static File getOldSourceLocation(BundleDescription bundle) {
		Properties props = (Properties) bundle.getUserObject();
		if (props == null || !props.containsKey(OLD_BUNDLE_LOCATION))
			return null;

		String oldBundleLocation = props.getProperty(OLD_BUNDLE_LOCATION);
		if (oldBundleLocation != null) {
			File previousSrcRoot = new File(oldBundleLocation, "src"); //$NON-NLS-1$
			if (previousSrcRoot.exists())
				return previousSrcRoot;
		}

		return null;
	}

	public static Map parseSourceBundleEntry(BundleDescription bundle) {
		String header = getSourceBundleHeader(bundle);
		if (header.length() == 0)
			return Collections.EMPTY_MAP;

		HashMap map = new HashMap();
		ManifestElement[] elements;
		try {
			elements = ManifestElement.parseHeader(ECLIPSE_SOURCE_BUNDLE, header);
		} catch (BundleException e1) {
			return Collections.EMPTY_MAP;
		}
		for (int i = 0; i < elements.length; i++) {
			String key = elements[i].getValue();
			HashMap subMap = new HashMap(2);
			map.put(key, subMap);
			for (Enumeration e = elements[i].getDirectiveKeys(); e != null && e.hasMoreElements();) {
				String directive = (String) e.nextElement();
				subMap.put(directive, elements[i].getDirective(directive));
			}
			for (Enumeration e = elements[i].getKeys(); e != null && e.hasMoreElements();) {
				String attribute = (String) e.nextElement();
				subMap.put(attribute, elements[i].getAttribute(attribute));
			}
		}
		return map;
	}

	public static final String EXTRA_ID = "id"; //$NON-NLS-1$
	public static final String EXTRA_VERSION = "version"; //$NON-NLS-1$
	public static final String EXTRA_UNPACK = "unpack"; //$NON-NLS-1$
	public static final String EXTRA_OPTIONAL = "optional"; //$NON-NLS-1$
	public static final String EXTRA_OS = "os"; //$NON-NLS-1$
	public static final String EXTRA_WS = "ws"; //$NON-NLS-1$
	public static final String EXTRA_ARCH = "arch"; //$NON-NLS-1$

	public static Map parseExtraBundlesString(String input, boolean onlyId) {
		Map results = new HashMap();
		StringTokenizer tokenizer = null;
		if (onlyId)
			if (input.startsWith("plugin@")) //$NON-NLS-1$
				tokenizer = new StringTokenizer(input.substring(7), ";"); //$NON-NLS-1$
			else if (input.startsWith("exclude@") || input.startsWith("feature@")) //$NON-NLS-1$ //$NON-NLS-2$
				tokenizer = new StringTokenizer(input.substring(8), ";"); //$NON-NLS-1$
			else
				tokenizer = new StringTokenizer(input, ";"); //$NON-NLS-1$
		else
			tokenizer = new StringTokenizer(input, ";"); //$NON-NLS-1$

		results.put(EXTRA_ID, tokenizer.nextToken());
		results.put(EXTRA_VERSION, Version.emptyVersion);
		results.put(EXTRA_UNPACK, Boolean.TRUE);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			String value = null;
			int idx = token.indexOf('=');
			if (idx > 0 && idx < token.length() - 1) {
				value = token.substring(idx + 1).trim();
				if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
					value = value.substring(1, value.length() - 1);
			}
			if (token.startsWith(EXTRA_VERSION))
				results.put(EXTRA_VERSION, new Version(value));
			else if (token.startsWith(EXTRA_UNPACK))
				results.put(EXTRA_UNPACK, Boolean.valueOf(value));
			else if (token.startsWith(EXTRA_OS))
				results.put(EXTRA_OS, value);
			else if (token.startsWith(EXTRA_WS))
				results.put(EXTRA_WS, value);
			else if (token.startsWith(EXTRA_ARCH))
				results.put(EXTRA_ARCH, value);
			else if (token.startsWith(EXTRA_OPTIONAL))
				results.put(EXTRA_OPTIONAL, Boolean.valueOf(value));
		}
		return results;
	}

	static public boolean matchVersions(String version1, String version2) {
		if (version1 == null)
			version1 = GENERIC_VERSION_NUMBER;
		if (version2 == null)
			version2 = GENERIC_VERSION_NUMBER;

		if (version1.equals(version2) || version1.equals(GENERIC_VERSION_NUMBER) || version2.equals(GENERIC_VERSION_NUMBER))
			return true;

		if (version1.endsWith(PROPERTY_QUALIFIER) || version2.endsWith(PROPERTY_QUALIFIER)) {
			int idx = version1.indexOf(PROPERTY_QUALIFIER);
			if (idx > -1)
				version1 = version1.substring(0, idx);
			idx = version2.indexOf(PROPERTY_QUALIFIER);

			version1 = version1.substring(0, idx);
			return (version1.length() > version2.length()) ? version1.startsWith(version2) : version2.startsWith(version1);
		}

		return false;
	}

	/**
	 * 
	 * @param buf
	 * @param start
	 * @param target
	 * @return int
	 */
	static public int scan(StringBuffer buf, int start, String target) {
		return scan(buf, start, new String[] {target});
	}

	/**
	 * 
	 * @param buf
	 * @param start
	 * @param targets
	 * @return int
	 */
	static public int scan(StringBuffer buf, int start, String[] targets) {
		for (int i = start; i < buf.length(); i++) {
			for (int j = 0; j < targets.length; j++) {
				if (i < buf.length() - targets[j].length()) {
					String match = buf.substring(i, i + targets[j].length());
					if (targets[j].equals(match))
						return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Return a buffer containing the contents of the file at the specified location.
	 * 
	 * @param target the file
	 * @return StringBuffer
	 * @throws IOException
	 */
	static public StringBuffer readFile(File target) throws IOException {
		return readFile(new FileInputStream(target));
	}

	static public StringBuffer readFile(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(new BufferedInputStream(stream));
		StringBuffer result = new StringBuffer();
		char[] buf = new char[4096];
		int count;
		try {
			count = reader.read(buf, 0, buf.length);
			while (count != -1) {
				result.append(buf, 0, count);
				count = reader.read(buf, 0, buf.length);
			}
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore exceptions here
			}
		}
		return result;
	}

	/**
	 * Custom build scripts should have their version number matching the
	 * version number defined by the feature/plugin/fragment descriptor.
	 * This is a best effort job so do not worry if the expected tags were
	 * not found and just return without modifying the file.
	 * 
	 * @param buildFile
	 * @param propertyName
	 * @param version
	 * @throws IOException
	 *
	 */
	public static void updateVersion(File buildFile, String propertyName, String version) throws IOException {
		StringBuffer buffer = readFile(buildFile);
		int pos = scan(buffer, 0, propertyName);
		if (pos == -1)
			return;
		pos = scan(buffer, pos, "value"); //$NON-NLS-1$
		if (pos == -1)
			return;
		int begin = scan(buffer, pos, "\""); //$NON-NLS-1$
		if (begin == -1)
			return;
		begin++;
		int end = scan(buffer, begin, "\""); //$NON-NLS-1$
		if (end == -1)
			return;
		String currentVersion = buffer.substring(begin, end);
		String newVersion = version;
		if (currentVersion.equals(newVersion))
			return;
		buffer.replace(begin, end, newVersion);
		transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(buildFile));
	}

	public static Enumeration getArrayEnumerator(Object[] array) {
		return new ArrayEnumeration(array);
	}

	public static void close(Object obj) {
		if (obj == null)
			return;
		try {
			if (obj instanceof InputStream)
				((InputStream) obj).close();
			else if (obj instanceof ZipFile)
				((ZipFile) obj).close();
			else if (obj instanceof OutputStream)
				((OutputStream) obj).close();
		} catch (IOException e) {
			//boo
		}
	}

	public static boolean guessUnpack(BundleDescription bundle, String[] classpath) {
		return org.eclipse.pde.internal.publishing.Utils.guessUnpack(bundle, classpath);
	}

	public static Version extract3Segments(String s) {
		Version tmp = new Version(s);
		return new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
	}

	private static boolean needsReplacement(String s) {
		if (s.equalsIgnoreCase(GENERIC_VERSION_NUMBER) || s.endsWith(PROPERTY_QUALIFIER))
			return true;
		return false;
	}

	public static String getEntryVersionMappings(FeatureEntry[] entries, BuildTimeSite site) {
		return getEntryVersionMappings(entries, site, null);
	}

	public static String getEntryVersionMappings(FeatureEntry[] entries, BuildTimeSite site, AssemblyInformation assembly) {
		if (entries == null || site == null)
			return null;

		StringBuffer result = new StringBuffer();
		for (int i = 0; i < entries.length; i++) {
			String versionRequested = entries[i].getVersion();
			if (versionRequested == null)
				versionRequested = GENERIC_VERSION_NUMBER;
			String id = entries[i].getId();
			String newVersion = null;

			if (!needsReplacement(versionRequested))
				continue;

			try {
				if (entries[i].isPlugin()) {
					BundleDescription model = null;
					if (assembly != null)
						model = assembly.getPlugin(entries[i].getId(), versionRequested);
					if (model == null)
						model = site.getRegistry().getResolvedBundle(id, versionRequested);
					if (model != null)
						newVersion = model.getVersion().toString();
				} else {
					BuildTimeFeature feature = site.findFeature(id, versionRequested, false);
					if (feature != null)
						newVersion = feature.getVersion();
				}
			} catch (CoreException e) {
				continue;
			}
			if (newVersion != null) {
				result.append(id);
				result.append(':');
				result.append(extract3Segments(versionRequested));
				result.append(',');
				result.append(newVersion);
				result.append(',');
			}
		}
		return result.toString();
	}
}
