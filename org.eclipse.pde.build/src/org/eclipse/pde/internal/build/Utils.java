/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;
import org.osgi.framework.Version;

/**
 * General utility class.
 */
public final class Utils implements IPDEBuildConstants, IBuildPropertiesConstants, IXMLConstants {
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

	public static List extractPlugins(List initialList, List toExtract) {
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

	public static void generatePermissions(Properties featureProperties, Config aConfig, String targetRootProperty, AntScript script) {
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

	public static String[] getSourceBundleHeader(BundleDescription bundle) {
		Properties bundleProperties = (Properties) bundle.getUserObject();
		if (bundleProperties == null || !bundleProperties.containsKey(ECLIPSE_SOURCE_BUNDLE))
			return new String[0];

		String header = bundleProperties.getProperty(ECLIPSE_SOURCE_BUNDLE);
		return getArrayFromString(header);
	}

	public static Map parseSourceBundleEntry(BundleDescription bundle) {
		String[] header = getSourceBundleHeader(bundle);
		if (header.length > 0) {
			HashMap map = new HashMap();
			for (int i = 0; i < header.length; i++) {
				String[] args = getArrayFromString(header[i], ";"); //$NON-NLS-1$

				if (args.length == 1) {
					map.put(args[0], Collections.EMPTY_MAP);
				} else {
					HashMap subMap = new HashMap(2);
					map.put(args[0], subMap);
					for (int j = 1; j < args.length; j++) {
						int idx = args[j].indexOf('=');
						if (idx != -1) {
							subMap.put(args[j].substring(0, idx), args[j].substring(idx, args[j].length()));
						} else {
							subMap.put(args[j], ""); //$NON-NLS-1$
						}
					}
				}
			}
			return map;
		}
		return Collections.EMPTY_MAP;
	}

	public static Object[] parseExtraBundlesString(String input, boolean onlyId) {
		StringTokenizer tokenizer = null;
		if (onlyId)
			tokenizer = new StringTokenizer(input.startsWith("plugin@") ? input.substring(7) : input.substring(8), ";"); //$NON-NLS-1$//$NON-NLS-2$
		else
			tokenizer = new StringTokenizer(input, ";"); //$NON-NLS-1$
		String bundleId = tokenizer.nextToken();
		Version version = Version.emptyVersion;
		Boolean unpack = Boolean.FALSE;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.startsWith("version")) { //$NON-NLS-1$
				version = new Version(token.substring(8));
				continue;
			}
			if (token.startsWith("unpack")) { //$NON-NLS-1$
				unpack = (token.toLowerCase().indexOf(TRUE) > -1) ? Boolean.TRUE : Boolean.FALSE;
				continue;
			}
		}
		return new Object[] {bundleId, version, unpack};
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
}
