/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.core.runtime.model.PluginPrerequisiteModel;

/**
 * General utility class.
 */
public final class Utils implements IPDEBuildConstants {

/**
 * Convert a list of tokens into an array. The list separator has to be specified.
 */
public static String[] getArrayFromString(String list, String separator) {
	if (list == null || list.trim().equals("")) //$NON-NLS-1$
		return new String[0];
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals("")) //$NON-NLS-1$
			result.add(token);
	}
	return (String[]) result.toArray(new String[result.size()]);
}

/**
 * Return a string array constructed from the given list of comma-separated tokens. 
 *  * @param list the list to convert * @return the array of strings */
public static String[] getArrayFromString(String list) {
	return getArrayFromString(list, ","); //$NON-NLS-1$
}

/**
 * Finds out if an status has the given severity. In case of a multi status,
 * its children are also included.
 *  * @param status * @param severity * @return boolean */
public static boolean contains(IStatus status, int severity) {
	if (status.matches(severity))
		return true;
	if (status.isMultiStatus()) {
		IStatus[] children = status.getChildren();
		for (int i = 0; i < children.length; i++)
			if (contains(children[i], severity))
				return true;
	}
	return false;
}

/**
 * Converts an array of strings into an array of URLs.
 *  * @param target * @return URL[] * @throws CoreException */
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

/**
 * Return a string which is a concatination of each member of the given collection,
 * separated by the given separator.
 *  * @param collection the collection to concatinate * @param separator the separator to use * @return String */
public static String getStringFromCollection(Collection collection, String separator) {
	StringBuffer result = new StringBuffer();
	boolean first = true;
	for (Iterator i = collection.iterator(); i.hasNext();) {
		if (!first)
			result.append(separator);
		first = false;
		result.append(i.next().toString());
	}
	return result.toString();
}

/**
 * Return a string which is a concatination of each member of the given array,
 * separated by the given separator.
 *  * @param values the array to concatinate * @param separator the separator to use * @return String */
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
 *  * @param plugins * @return String[][] */
public static String[][] computePrerequisiteOrder(PluginModel[] plugins) {
	List prereqs = new ArrayList(9);
	Set pluginList = new HashSet(plugins.length);
	for (int i = 0; i < plugins.length; i++) 
		pluginList.add(plugins[i].getId());
	// create a collection of directed edges from plugin to prereq
	for (int i = 0; i < plugins.length; i++) {
		boolean boot = false;
		boolean runtime = false;
		boolean found = false;
		PluginPrerequisiteModel[] prereqList = plugins[i].getRequires();
		if (prereqList != null) {
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				String prereq = prereqList[j].getPlugin();
				boot = boot || prereq.equals(BootLoader.PI_BOOT);
				runtime = runtime || prereq.equals(Platform.PI_RUNTIME);
				if (pluginList.contains(prereq)) {
					found = true;
					prereqs.add(new String[] { plugins[i].getId(), prereq });
				}
			}
		}
		// if we didn't find any prereqs for this plugin, add a null prereq 
		// to ensure the value is in the output
		if (!found)
			prereqs.add(new String[] { plugins[i].getId(), null });
		// if we didn't find the boot or runtime plugins as prereqs and they are in the list
		// of plugins to build, add prereq relations for them.  This is required since the 
		// boot and runtime are implicitly added to a plugin's requires list by the platform runtime.
		// Note that we should skip the xerces plugin as this would cause a circularity.
		if (plugins[i].getId().equals("org.apache.xerces")) //$NON-NLS-1$
			continue;
		if (!boot && pluginList.contains(BootLoader.PI_BOOT) && !plugins[i].getId().equals(BootLoader.PI_BOOT))
			prereqs.add(new String[] { plugins[i].getId(), BootLoader.PI_BOOT});
		if (!runtime && pluginList.contains(Platform.PI_RUNTIME) && !plugins[i].getId().equals(Platform.PI_RUNTIME) && !plugins[i].getId().equals(BootLoader.PI_BOOT))
			prereqs.add(new String[] { plugins[i].getId(), Platform.PI_RUNTIME});
	}
	// do a topological sort and return the prereqs
	String[][] prereqArray = (String[][]) prereqs.toArray(new String[prereqs.size()][]);
	return computeNodeOrder(prereqArray);
}

/**
 *  * @param specs * @return String[][] */
protected static String[][] computeNodeOrder(String[][] specs) {
	HashMap counts = computeCounts(specs);
	List nodes = new ArrayList(counts.size());
	while (!counts.isEmpty()) {
		List roots = findRootNodes(counts);
		if (roots.isEmpty())
			break;
		for (Iterator i = roots.iterator(); i.hasNext();)
			counts.remove(i.next());
		nodes.addAll(roots);
		removeArcs(specs, roots, counts);
	}
	String[][] result = new String[2][];
	result[0] = (String[]) nodes.toArray(new String[nodes.size()]);
	result[1] = (String[]) counts.keySet().toArray(new String[counts.size()]);
	return result;
}

/**
 *  * @param counts * @return List */
protected static List findRootNodes(HashMap counts) {
	List result = new ArrayList(5);
	for (Iterator i = counts.keySet().iterator(); i.hasNext();) {
		String node = (String) i.next();
		int count = ((Integer) counts.get(node)).intValue();
		if (count == 0)
			result.add(node);
	}
	return result;
}

/**
 *  * @param mappings * @param roots * @param counts */
protected static void removeArcs(String[][] mappings, List roots, HashMap counts) {
	for (Iterator j = roots.iterator(); j.hasNext();) {
		String root = (String) j.next();
		for (int i = 0; i < mappings.length; i++) {
			if (root.equals(mappings[i][1])) {
				String input = mappings[i][0];
				Integer count = (Integer) counts.get(input);
				if (count != null)
					counts.put(input, new Integer(count.intValue() - 1));
			}
		}
	}
}

/**
 *  * @param mappings * @return HashMap */
protected static HashMap computeCounts(String[][] mappings) {
	HashMap counts = new HashMap(5);
	for (int i = 0; i < mappings.length; i++) {
		String from = mappings[i][0];
		Integer fromCount = (Integer) counts.get(from);
		String to = mappings[i][1];
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

/**
 * Helper method to ensure an array is converted into an ArrayList.
 *  * @param args * @return ArrayList */
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
 * Return a path which is equivalent to the given location relative to the specified
 * base path.
 *  * @param location the location to convert * @param base the base path * @return IPath */
public static IPath makeRelative(IPath location, IPath base) {
	IPath result = location;
	if (result.getDevice() != null && !result.getDevice().equalsIgnoreCase(base.getDevice()))
		return location;
	int baseCount = base.segmentCount();
	int count = base.matchingFirstSegments(result);
	String temp = ""; //$NON-NLS-1$
	for (int j = 0; j < baseCount - count; j++)
		temp += "../"; //$NON-NLS-1$
	result = new Path(temp).append(result.removeFirstSegments(count));
	return result;
}

/**
 * Transfers all available bytes from the given input stream to the given output stream. 
 * Regardless of failure, this method closes both streams.
 *  * @param source * @param destination * @throws IOException */
public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
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
		}
		try {
			destination.close();
		} catch (IOException e) {
		}
	}
}
}