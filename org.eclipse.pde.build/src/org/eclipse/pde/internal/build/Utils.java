/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IPluginEntry;

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
			if (i > 0)
				result.append(separator);
			result.append(values[i]);
		}
		return result.toString();
	}

	/**
	 * 
	 * @param counts
	 * @return List
	 */
	protected static List findRootNodes(Map counts) {
		List result = new ArrayList(5);
		for (Iterator i = counts.keySet().iterator(); i.hasNext();) {
			Object node = i.next();
			int count = ((Integer) counts.get(node)).intValue();
			if (count == 0)
				result.add(node);
		}
		return result;
	}

	/**
	 * Helper method to ensure an array is converted into an ArrayList.
	 * 
	 * @param args
	 * @return List
	 */
	public static ArrayList getArrayList(Object[] args) {
		// We could be using Arrays.asList() here, but it does not specify
		// what kind of list it will return. We do need a list that
		// implements the method List.remove(int) and ArrayList does.
		ArrayList result = new ArrayList(args.length);
		for (int i = 0; i < args.length; i++)
			result.add(args[i]);
		return result;
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
		if (location.getDevice() != null && !location.getDevice().equalsIgnoreCase(base.getDevice()))
			return location;
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

	public static IPluginEntry[] getPluginEntry(IFeature feature, String pluginId, boolean raw) {
		IPluginEntry[] plugins;
		if (raw)
			plugins = feature.getRawPluginEntries();
		else
			plugins = feature.getPluginEntries();
		List foundEntries = new ArrayList(5);

		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].getVersionedIdentifier().getIdentifier().equals(pluginId))
				foundEntries.add(plugins[i]);
		}
		return (IPluginEntry[]) foundEntries.toArray(new IPluginEntry[foundEntries.size()]);

	}

	// Return a collection of File, the result can be null
	public static Collection findFiles(String from, String foldername, final String filename) {
		// if from is a file which name match filename, then simply return the
		// file
		File root = new File(from);
		if (root.isFile() && root.getName().equals(filename)) {
			Collection coll = new ArrayList(1);
			coll.add(root);
			return coll;
		}

		String featureDirectory = from + '/' + foldername; 
		Collection collectedElements = new ArrayList(10);

		File[] featureDirectoryContent = new File(featureDirectory).listFiles();
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

	public static boolean isIn(IPluginEntry[] array, IPluginEntry element) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].getVersionedIdentifier().equals(element.getVersionedIdentifier()))
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
					if (files[i].isDirectory())
						continue;

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

	static private class Relation {
		Object from;
		Object to;

		Relation(Object from, Object to) {
			this.from = from;
			this.to = to;
		}

		public String toString() {
			return from.toString() + "->" + (to == null ? "" : to.toString()); //$NON-NLS-1$//$NON-NLS-2$
		}
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

	public static List computePrerequisiteOrder(List plugins) {
		List prereqs = new ArrayList(plugins.size());
		List fragments = new ArrayList();

		// create a collection of directed edges from plugin to prereq
		for (Iterator iter = plugins.iterator(); iter.hasNext();) {
			BundleDescription current = (BundleDescription) iter.next();
			if (current.getHost() != null) {
				fragments.add(current);
				continue;
			}
			boolean found = false;

			BundleDescription[] prereqList = PDEState.getDependentBundles(current);
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				if (plugins.contains(prereqList[j])) {
					found = true;
					prereqs.add(new Relation(current, prereqList[j]));
				}
			}

			// if we didn't find any prereqs for this plugin, add a null prereq
			// to ensure the value is in the output
			if (!found)
				prereqs.add(new Relation(current, null));
		}

		//The fragments needs to added relatively to their host and to their
		// own prerequisite (bug #43244)
		for (Iterator iter = fragments.iterator(); iter.hasNext();) {
			BundleDescription current = (BundleDescription) iter.next();

			if (plugins.contains(current.getHost().getBundle()))
				prereqs.add(new Relation(current, current.getHost().getSupplier()));
			else
				BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_GENERIC, NLS.bind(Messages.exception_hostNotFound, current.getSymbolicName()), null));

			BundleDescription[] prereqList = PDEState.getDependentBundles(current);
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				if (plugins.contains(prereqList[j])) {
					prereqs.add(new Relation(current, prereqList[j]));
				}
			}
		}

		// do a topological sort, insert the fragments into the sorted elements
		return computeNodeOrder(prereqs);
	}

	protected static List computeNodeOrder(List edges) {
		Map counts = computeCounts(edges);
		List nodes = new ArrayList(counts.size());
		while (!counts.isEmpty()) {
			List roots = findRootNodes(counts);
			if (roots.isEmpty())
				break;
			for (Iterator i = roots.iterator(); i.hasNext();)
				counts.remove(i.next());
			nodes.addAll(roots);
			removeArcs(edges, roots, counts);
		}
		return nodes;
	}

	protected static Map computeCounts(List mappings) {
		Map counts = new HashMap(5);
		for (int i = 0; i < mappings.size(); i++) {
			Object from = ((Relation) mappings.get(i)).from;
			Integer fromCount = (Integer) counts.get(from);
			Object to = ((Relation) mappings.get(i)).to;
			if (to == null)
				counts.put(from, new Integer(0));
			else {
				if (((Integer) counts.get(to)) == null)
					counts.put(to, new Integer(0));
				fromCount = fromCount == null ? new Integer(1) : new Integer(fromCount.intValue() + 1);
				counts.put(from, fromCount);
			}
		}
		return counts;
	}

	protected static void removeArcs(List edges, List roots, Map counts) {
		for (Iterator j = roots.iterator(); j.hasNext();) {
			Object root = j.next();
			for (int i = 0; i < edges.size(); i++) {
				if (root.equals(((Relation) edges.get(i)).to)) {
					Object input = ((Relation) edges.get(i)).from;
					Integer count = (Integer) counts.get(input);
					if (count != null)
						counts.put(input, new Integer(count.intValue() - 1));
				}
			}
		}
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
		String prefixPermissions = ROOT_PREFIX + configInfix + '.' + PERMISSIONS + '.';
		String prefixLinks = ROOT_PREFIX + configInfix + '.' + LINK;
		String commonPermissions = ROOT_PREFIX + PERMISSIONS + '.';
		String commonLinks = ROOT_PREFIX + LINK;
		for (Iterator iter = featureProperties.entrySet().iterator(); iter.hasNext();) {
			Map.Entry permission = (Map.Entry) iter.next();
			String instruction = (String) permission.getKey();
			String parameters = removeEndingSlashes((String) permission.getValue());
			if (instruction.startsWith(prefixPermissions)) {
				generateChmodInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configInfix + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), instruction.substring(prefixPermissions.length()), parameters); 
				continue;
			}
			if (instruction.startsWith(prefixLinks)) {
				generateLinkInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configInfix + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), parameters);
				continue;
			}
			if (instruction.startsWith(commonPermissions)) {
				generateChmodInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configInfix + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), instruction.substring(commonPermissions.length()), parameters);
				continue;
			}
			if (instruction.startsWith(commonLinks)) {
				generateLinkInstruction(script, getPropertyFormat(targetRootProperty) + '/' + configInfix + '/' + getPropertyFormat(PROPERTY_COLLECTING_FOLDER), parameters);
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
			arguments.add("-s"); //$NON-NLS-1$
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
}
