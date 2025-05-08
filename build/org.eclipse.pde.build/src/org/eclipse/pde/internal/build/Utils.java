/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *      Tue Ton - support for FreeBSD
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeSite;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * General utility class.
 */
public final class Utils implements IPDEBuildConstants, IBuildPropertiesConstants, IXMLConstants {
	static class ArrayEnumeration implements Enumeration<Object> {
		private final Object[] array;
		int cur = 0;

		public ArrayEnumeration(Object[] array) {
			this.array = new Object[array.length];
			System.arraycopy(array, 0, this.array, 0, this.array.length);
		}

		@Override
		public boolean hasMoreElements() {
			return cur < array.length;
		}

		@Override
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

	public static final VersionRange EMPTY_RANGE = new VersionRange("0.0.0"); //$NON-NLS-1$

	public static VersionRange createVersionRange(String versionId) {
		VersionRange range = null;
		if (versionId == null || versionId.length() == 0 || GENERIC_VERSION_NUMBER.equals(versionId)) {
			range = EMPTY_RANGE;
		} else {
			int qualifierIdx = versionId.indexOf(IBuildPropertiesConstants.PROPERTY_QUALIFIER);
			if (qualifierIdx != -1) {
				String newVersion = versionId.substring(0, qualifierIdx);
				if (newVersion.endsWith(".")) { //$NON-NLS-1$
					newVersion = newVersion.substring(0, newVersion.length() - 1);
				}
				Version lower = new Version(newVersion);
				Version upper = null;
				String newQualifier = incrementQualifier(lower.getQualifier());
				if (newQualifier == null) {
					upper = new Version(lower.getMajor(), lower.getMinor(), lower.getMicro() + 1);
				} else {
					upper = new Version(lower.getMajor(), lower.getMinor(), lower.getMicro(), newQualifier);
				}
				range = new VersionRange(VersionRange.LEFT_CLOSED, lower, upper, VersionRange.RIGHT_OPEN);
			} else {
				range = new VersionRange(VersionRange.LEFT_CLOSED, new Version(versionId), new Version(versionId), VersionRange.RIGHT_CLOSED);
			}
		}
		return range;
	}

	public static VersionRange createVersionRange(String lowerBound, boolean includeLowerBound, String upperBound, boolean includeUpperBound) {
		char leftType = includeLowerBound ? VersionRange.LEFT_CLOSED : VersionRange.LEFT_OPEN;
		char rightType = includeUpperBound ? VersionRange.RIGHT_CLOSED : VersionRange.RIGHT_OPEN;
		return new VersionRange(leftType, Version.parseVersion(lowerBound), Version.parseVersion(upperBound), rightType);
	}

	public static VersionRange createExactVersionRange(Version version) {
		return new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED);
	}

	public static VersionRange parseVersionRange(String version) {
		return version != null && !version.isEmpty() ? new VersionRange(version) : EMPTY_RANGE;
	}

	public static VersionRange createVersionRange(FeatureEntry entry) {
		String versionSpec = entry.getVersion();
		if (versionSpec == null) {
			return EMPTY_RANGE;
		}
		Version version = new Version(versionSpec);
		if (version.equals(Version.emptyVersion)) {
			return EMPTY_RANGE;
		}
		String match = entry.getMatch();
		if (!entry.isRequires() || match == null) {
			return createVersionRange(versionSpec);
		}

		if (match.equals("perfect")) { //$NON-NLS-1$
			return new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED);
		}
		if (match.equals("equivalent")) { //$NON-NLS-1$
			Version upper = new Version(version.getMajor(), version.getMinor() + 1, 0);
			return new VersionRange(VersionRange.LEFT_CLOSED, version, upper, VersionRange.RIGHT_OPEN);
		}
		if (match.equals("compatible")) { //$NON-NLS-1$
			Version upper = new Version(version.getMajor() + 1, 0, 0);
			return new VersionRange(VersionRange.LEFT_CLOSED, version, upper, VersionRange.RIGHT_OPEN);
		}
		if (match.equals("greaterOrEqual")) { //$NON-NLS-1$
			return new VersionRange(VersionRange.LEFT_CLOSED, version, null, VersionRange.RIGHT_CLOSED);
		}

		return EMPTY_RANGE;
	}

	private static String incrementQualifier(String qualifier) {
		int idx = qualifier.length() - 1;

		for (; idx >= 0; idx--) {
			//finding last non-'z' character
			if (qualifier.charAt(idx) != 'z') {
				break;
			}
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
		if (list == null || list.trim().equals("")) { //$NON-NLS-1$
			return new String[0];
		}
		List<String> result = new ArrayList<>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) { //$NON-NLS-1$
				result.add(token);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified. The spcecificity of this method is that it returns an empty
	 * element when to same separators are following each others. For example
	 * the string a,,b returns the following array [a, ,b]
	 */
	public static String[] getArrayFromStringWithBlank(String list, String separator) {
		if (list == null || list.trim().length() == 0) {
			return new String[0];
		}
		List<String> result = new ArrayList<>();
		boolean previousWasSeparator = true;
		for (StringTokenizer tokens = new StringTokenizer(list, separator, true); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (token.equals(separator)) {
				if (previousWasSeparator) {
					result.add(""); //$NON-NLS-1$
				}
				previousWasSeparator = true;
			} else {
				result.add(token);
				previousWasSeparator = false;
			}
		}
		return result.toArray(new String[result.size()]);
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
	 * Return a string which is a concatination of each member of the given
	 * collection, separated by the given separator.
	 * 
	 * @param collection
	 *            the collection to concatinate
	 * @param separator
	 *            the separator to use
	 * @return String
	 */
	public static String getStringFromCollection(Collection<?> collection, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (Object name : collection) {
			if (first) {
				first = false;
			} else {
				result.append(separator);
			}
			result.append(name);
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
				if (i > 0) {
					result.append(separator);
				}
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
			if (base.getDevice() != null) {
				return location;
			}
		} else {
			if (!location.getDevice().equalsIgnoreCase(base.getDevice())) {
				return location;
			}
		}
		int baseCount = base.segmentCount();
		int count = base.matchingFirstSegments(location);
		String temp = ""; //$NON-NLS-1$
		for (int j = 0; j < baseCount - count; j++) {
			temp += "../"; //$NON-NLS-1$
		}
		return IPath.fromOSString(temp).append(location.removeFirstSegments(count));
	}

	static public void copyFile(String src, String dest) throws IOException {
		Path source = Path.of(src);
		if (!Files.isRegularFile(source)) {
			return;
		}
		Path destination = Path.of(dest);
		Files.createDirectories(destination.getParent());
		Files.copy(source, destination);
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
		if (raw) {
			plugins = feature.getRawPluginEntries();
		} else {
			plugins = feature.getPluginEntries();
		}
		List<FeatureEntry> foundEntries = new ArrayList<>(5);

		for (FeatureEntry plugin2 : plugins) {
			if (plugin2.getId().equals(pluginId)) {
				foundEntries.add(plugin2);
			}
		}
		return foundEntries.toArray(new FeatureEntry[foundEntries.size()]);

	}

	// Return a collection of File, the result can be null
	public static Collection<File> findFiles(File from, String foldername, final String filename) {
		// if from is a file which name match filename, then simply return the
		// file
		File root = from;
		if (root.isFile() && root.getName().equals(filename)) {
			Collection<File> coll = new ArrayList<>(1);
			coll.add(root);
			return coll;
		}

		Collection<File> collectedElements = new ArrayList<>(10);

		File[] featureDirectoryContent = new File(from, foldername).listFiles();
		if (featureDirectoryContent == null) {
			return null;
		}

		for (File element : featureDirectoryContent) {
			if (element.isDirectory()) {
				File[] featureFiles = element.listFiles((FilenameFilter) (dir, name) -> name.equals(filename));
				if (featureFiles.length != 0) {
					collectedElements.add(featureFiles[0]);
				}
			}
		}
		return collectedElements;
	}

	public static boolean isIn(FeatureEntry[] array, FeatureEntry element) {
		for (FeatureEntry element2 : array) {
			if (element2.getId().equals(element.getId()) && element2.getVersion().equals(element.getVersion())) {
				return true;
			}
		}
		return false;
	}

	public static Collection<String> copyFiles(String fromDir, String toDir) throws CoreException {
		File templateLocation = new File(fromDir);
		Collection<String> copiedFiles = new ArrayList<>();
		if (templateLocation.exists()) {
			File[] files = templateLocation.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						File subDir = new File(toDir, file.getName());
						if (!subDir.exists()) {
							subDir.mkdirs();
						}
						Collection<String> subFiles = copyFiles(fromDir + '/' + file.getName(), toDir + '/' + file.getName());
						for (String sub : subFiles) {
							copiedFiles.add(file.getName() + '/' + sub);
						}
						continue;
					}
					String fileToCopy = toDir + '/' + file.getName();
					try {
						Files.copy(file.toPath(), Path.of(fileToCopy), StandardCopyOption.REPLACE_EXISTING);
						copiedFiles.add(file.getName());
					} catch (IOException e) {
						String message = NLS.bind(Messages.exception_writingFile, fileToCopy);
						throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
					}
				}
			}
		}
		return copiedFiles;
	}

	public static List<BundleDescription> extractPlugins(List<BundleDescription> initialList, Set<BundleDescription> toExtract) {
		//TODO This algorithm needs to be  improved
		if (initialList.size() == toExtract.size()) {
			return initialList;
		}
		List<BundleDescription> result = new ArrayList<>(toExtract.size());
		for (BundleDescription element : initialList) {
			if (toExtract.contains(element)) {
				result.add(element);
				if (result.size() == toExtract.size()) {
					break;
				}
			}
		}
		return result;
	}

	public static int isStringIn(String[] searched, String toSearch) {
		if (searched == null || toSearch == null) {
			return -1;
		}
		for (int i = 0; i < searched.length; i++) {
			if (toSearch.startsWith(searched[i])) {
				return i;
			}
		}
		return -1;
	}

	static public Properties getOldExecutableRootOverrides() {
		Properties overrides = new Properties();
		overrides.put("root.win32.win32.x86_64", "file:bin/win32/win32/x86_64/launcher.exe"); //$NON-NLS-1$ //$NON-NLS-2$
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
	static public Map<String, Map<String, String>> processRootProperties(Properties properties, boolean mergeCommon) {
		Map<String, Map<String, String>> map = new HashMap<>();
		Map<String, String> common = new HashMap<>();
		for (Enumeration<Object> keys = properties.keys(); keys.hasMoreElements();) {
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
				Map<String, String> submap = (config.length() == 0) ? common : map.get(config);
				if (submap == null) {
					submap = new HashMap<>();
					map.put(config, submap);
				}
				if (submap.containsKey(entryKey)) {
					String existing = submap.get(entryKey);
					submap.put(entryKey, existing + "," + properties.getProperty(entry)); //$NON-NLS-1$
				} else {
					submap.put(entryKey, (String) properties.get(entry));
				}
			}
		}

		//merge the common properties into each of the configs
		if (common.size() > 0 && mergeCommon) {
			for (String key : map.keySet()) {
				Map<String, String> submap = map.get(key);
				for (String commonKey : common.keySet()) {
					if (submap.containsKey(commonKey)) {
						String existing = submap.get(commonKey);
						submap.put(commonKey, existing + "," + common.get(commonKey)); //$NON-NLS-1$
					} else {
						submap.put(commonKey, common.get(commonKey));
					}
				}
			}
		}

		//and also add the common properties independently
		if (mergeCommon || common.size() > 0) {
			map.put(ROOT_COMMON, common);
		}
		return map;
	}

	public static void generatePermissions(Properties featureProperties, Config aConfig, String targetRootProperty, AntScript script) {
		if (featureProperties == null) {
			return;
		}
		String configInfix = aConfig.toString("."); //$NON-NLS-1$
		String configPath = aConfig.toStringReplacingAny(".", ANY_STRING); //$NON-NLS-1$
		String prefixPermissions = ROOT_PREFIX + configInfix + '.' + PERMISSIONS + '.';
		String prefixLinks = ROOT_PREFIX + configInfix + '.' + LINK;
		String commonPermissions = ROOT_PREFIX + PERMISSIONS + '.';
		String commonLinks = ROOT_PREFIX + LINK;
		for (Entry<Object, Object> permission : featureProperties.entrySet()) {
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
			if (params[i].endsWith("/")) { //$NON-NLS-1$
				params[i] = params[i].substring(0, params[i].length() - 1);
			}
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
		List<String> arguments = new ArrayList<>(2);
		for (int i = 0; i < links.length; i += 2) {
			arguments.add("-sf"); //$NON-NLS-1$
			arguments.add(links[i]);
			arguments.add(links[i + 1]);
			script.printExecTask("ln", dir, arguments, "Linux,FreeBSD"); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (bundleProperties == null || !bundleProperties.containsKey(ECLIPSE_SOURCE_BUNDLE)) {
			return ""; //$NON-NLS-1$
		}

		String header = bundleProperties.getProperty(ECLIPSE_SOURCE_BUNDLE);
		return header;
	}

	/**
	 * Given a newly generated old-style source bundle for which there was a previously existing
	 * version in the target, return the location of the src folder in that earlier version
	 * @return the old version's src folder, or null
	 */
	public static File getOldSourceLocation(BundleDescription bundle) {
		Properties props = (Properties) bundle.getUserObject();
		if (props == null || !props.containsKey(OLD_BUNDLE_LOCATION)) {
			return null;
		}

		String oldBundleLocation = props.getProperty(OLD_BUNDLE_LOCATION);
		if (oldBundleLocation != null) {
			File previousSrcRoot = new File(oldBundleLocation, "src"); //$NON-NLS-1$
			if (previousSrcRoot.exists()) {
				return previousSrcRoot;
			}
		}

		return null;
	}

	public static Map<String, Map<String, String>> parseSourceBundleEntry(BundleDescription bundle) {
		String header = getSourceBundleHeader(bundle);
		if (header.length() == 0) {
			return Collections.emptyMap();
		}

		HashMap<String, Map<String, String>> map = new HashMap<>();
		ManifestElement[] elements;
		try {
			elements = ManifestElement.parseHeader(ECLIPSE_SOURCE_BUNDLE, header);
		} catch (BundleException e1) {
			return Collections.emptyMap();
		}
		for (ManifestElement element : elements) {
			String key = element.getValue();
			HashMap<String, String> subMap = new HashMap<>(2);
			map.put(key, subMap);
			for (Enumeration<String> e = element.getDirectiveKeys(); e != null && e.hasMoreElements();) {
				String directive = e.nextElement();
				subMap.put(directive, element.getDirective(directive));
			}
			for (Enumeration<String> e = element.getKeys(); e != null && e.hasMoreElements();) {
				String attribute = e.nextElement();
				subMap.put(attribute, element.getAttribute(attribute));
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

	public static Map<String, Object> parseExtraBundlesString(String input, boolean onlyId) {
		Map<String, Object> results = new HashMap<>();
		StringTokenizer tokenizer = null;
		if (onlyId) {
			if (input.startsWith("plugin@")) { //$NON-NLS-1$
				tokenizer = new StringTokenizer(input.substring(7), ";"); //$NON-NLS-1$
			} else if (input.startsWith("exclude@") || input.startsWith("feature@")) { //$NON-NLS-1$ //$NON-NLS-2$
				tokenizer = new StringTokenizer(input.substring(8), ";"); //$NON-NLS-1$
			} else {
				tokenizer = new StringTokenizer(input, ";"); //$NON-NLS-1$
			}
		} else {
			tokenizer = new StringTokenizer(input, ";"); //$NON-NLS-1$
		}

		results.put(EXTRA_ID, tokenizer.nextToken());
		results.put(EXTRA_VERSION, Version.emptyVersion);
		results.put(EXTRA_UNPACK, Boolean.FALSE);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			String value = null;
			int idx = token.indexOf('=');
			if (idx > 0 && idx < token.length() - 1) {
				value = token.substring(idx + 1).trim();
				if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
					value = value.substring(1, value.length() - 1);
				}
			}
			if (token.startsWith(EXTRA_VERSION)) {
				results.put(EXTRA_VERSION, new Version(value));
			} else if (token.startsWith(EXTRA_UNPACK)) {
				results.put(EXTRA_UNPACK, Boolean.valueOf(value));
			} else if (token.startsWith(EXTRA_OS)) {
				results.put(EXTRA_OS, value);
			} else if (token.startsWith(EXTRA_WS)) {
				results.put(EXTRA_WS, value);
			} else if (token.startsWith(EXTRA_ARCH)) {
				results.put(EXTRA_ARCH, value);
			} else if (token.startsWith(EXTRA_OPTIONAL)) {
				results.put(EXTRA_OPTIONAL, Boolean.valueOf(value));
			}
		}
		return results;
	}

	static public boolean matchVersions(String version1, String version2) {
		if (version1 == null) {
			version1 = GENERIC_VERSION_NUMBER;
		}
		if (version2 == null) {
			version2 = GENERIC_VERSION_NUMBER;
		}

		if (version1.equals(version2) || version1.equals(GENERIC_VERSION_NUMBER) || version2.equals(GENERIC_VERSION_NUMBER)) {
			return true;
		}

		if (version1.endsWith(PROPERTY_QUALIFIER) || version2.endsWith(PROPERTY_QUALIFIER)) {
			int idx = version1.indexOf(PROPERTY_QUALIFIER);
			if (idx > -1) {
				version1 = version1.substring(0, idx);
			}
			idx = version2.indexOf(PROPERTY_QUALIFIER);

			version1 = version1.substring(0, idx);
			return (version1.length() > version2.length()) ? version1.startsWith(version2) : version2.startsWith(version1);
		}

		return false;
	}

	/**
	 * Custom build scripts should have their version number matching the
	 * version number defined by the feature/plugin/fragment descriptor.
	 * This is a best effort job so do not worry if the expected tags were
	 * not found and just return without modifying the file.
	 */
	public static void updateVersion(File buildFile, String propertyName, String newVersion) throws IOException {
		String value = Files.readString(buildFile.toPath());
		int pos = value.indexOf(propertyName);
		if (pos == -1) {
			return;
		}
		pos = value.indexOf("value", pos); //$NON-NLS-1$
		if (pos == -1) {
			return;
		}
		int begin = value.indexOf("\"", pos); //$NON-NLS-1$
		if (begin == -1) {
			return;
		}
		begin++;
		int end = value.indexOf("\"", begin); //$NON-NLS-1$
		if (end == -1) {
			return;
		}
		String currentVersion = value.substring(begin, end);
		if (!currentVersion.equals(newVersion)) {
			Files.writeString(buildFile.toPath(), new StringBuilder(value).replace(begin, end, newVersion));
		}
	}

	public static Enumeration<Object> getArrayEnumerator(Object[] array) {
		return new ArrayEnumeration(array);
	}

	public static void close(Object obj) {
		if (obj == null) {
			return;
		}
		try {
			if (obj instanceof InputStream) {
				((InputStream) obj).close();
			} else if (obj instanceof ZipFile) {
				((ZipFile) obj).close();
			} else if (obj instanceof OutputStream) {
				((OutputStream) obj).close();
			}
		} catch (IOException e) {
			//boo
		}
	}

	public static boolean guessUnpack(BundleDescription bundle, String[] classpath) {
		return org.eclipse.pde.internal.publishing.Utils.guessUnpack(bundle, Arrays.asList(classpath));
	}

	public static Version extract3Segments(String s) {
		Version tmp = new Version(s);
		return new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
	}

	private static boolean needsReplacement(String s) {
		if (s.equalsIgnoreCase(GENERIC_VERSION_NUMBER) || s.endsWith(PROPERTY_QUALIFIER)) {
			return true;
		}
		return false;
	}

	public static String getEntryVersionMappings(FeatureEntry[] entries, BuildTimeSite site) {
		return getEntryVersionMappings(entries, site, null);
	}

	public static String getEntryVersionMappings(FeatureEntry[] entries, BuildTimeSite site, AssemblyInformation assembly) {
		if (entries == null || site == null) {
			return null;
		}

		StringBuffer result = new StringBuffer();
		for (FeatureEntry entry : entries) {
			String versionRequested = entry.getVersion();
			if (versionRequested == null) {
				versionRequested = GENERIC_VERSION_NUMBER;
			}
			String id = entry.getId();
			String newVersion = null;

			if (!needsReplacement(versionRequested)) {
				continue;
			}

			try {
				if (entry.isPlugin()) {
					BundleDescription model = null;
					if (assembly != null) {
						model = assembly.getPlugin(entry.getId(), versionRequested);
					}
					if (model == null) {
						model = site.getRegistry().getResolvedBundle(id, versionRequested);
					}
					if (model != null) {
						newVersion = model.getVersion().toString();
					}
				} else {
					BuildTimeFeature feature = site.findFeature(id, versionRequested, false);
					if (feature != null) {
						newVersion = feature.getVersion();
					}
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
