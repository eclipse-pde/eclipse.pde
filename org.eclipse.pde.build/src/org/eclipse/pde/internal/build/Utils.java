package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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
	if (list == null || list.trim().equals(""))
		return new String[0];
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return (String[]) result.toArray(new String[result.size()]);
}

/**
 * convert a list of comma-separated tokens into an array
 */
public static String[] getArrayFromString(String list) {
	return getArrayFromString(list, ",");
}

/**
 * Substitutes a word in a sentence.
 */
public static String substituteWord(String sentence, String oldWord, String newWord) {
	int index = sentence.indexOf(oldWord);
	if (index == -1)
		return sentence;
	StringBuffer sb = new StringBuffer();
	sb.append(sentence.substring(0, index));
	sb.append(newWord);
	sb.append(sentence.substring(index + oldWord.length()));
	return sb.toString();
}

/**
 * Finds out if an status has the given severity. In case of a multi status,
 * its children are also included.
 */
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
 */
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
		if (plugins[i].getId().equals("org.apache.xerces"))
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
 * 
 */
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
 * 
 */
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
 * 
 */
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
 * 
 */
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
}