package org.eclipse.pde.internal.core;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.internal.boot.update.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import java.io.*;
import java.net.*;
import java.util.*;

public abstract class PluginTool implements IPlatformRunnable {
	protected boolean usage = false;
	private PluginRegistryModel registry = null;
	URL pluginPath = null;
	ArrayList plugins = new ArrayList(3);
	String install = null;
	private List devEntries = null;
	private Hashtable propertyValues = new Hashtable(9);
	
	public static final String FILENAME_PROPERTIES = "build.properties";
	private static final String SEPARATOR_VERSION = "_";
	private static final String USAGE = "-?";
	private static final String PLUGINS = "-plugins";
	private static final String INSTALL = "-install";
	private static final String DEV_ENTRIES = "-dev";
/**
 * Deletes all the files and directories from the given root down (inclusive).
 * Returns false if we could not delete some file or an exception occurred
 * at any point in the deletion.
 * Even if an exception occurs, a best effort is made to continue deleting.
 */
public static boolean clear(java.io.File root) {
	boolean result = true;
	if (root.isDirectory()) {
		String[] list = root.list();
		// for some unknown reason, list() can return null.  
		// Just skip the children If it does.
		if (list != null)
			for (int i = 0; i < list.length; i++)
				result &= clear(new java.io.File(root, list[i]));
	}
	try {
		if (root.exists())
			result &= root.delete();
	} catch (Exception e) {
		result = false;
	}
	return result;
}
/**
 * convert a list of comma-separated tokens into an array
 */
protected String[] getArrayFromString(String prop) {
	if (prop == null || prop.trim().equals(""))
		return new String[0];
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(prop, ","); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return (String[]) result.toArray(new String[result.size()]);
}

protected String getStringFromCollection(Collection list, String prefix, String suffix, String separator) {
	StringBuffer result = new StringBuffer();
	boolean first = true;
	for (Iterator i = list.iterator(); i.hasNext();) {
		if (!first)
			result.append(separator);
		first = false;
		result.append(prefix);
		result.append((String) i.next());
		result.append(suffix);
	}
	return result.toString();
}
protected PluginDescriptorModel[] getDescriptors() {
	PluginDescriptorModel[] descriptors = null;
	if (plugins == null || plugins.isEmpty())
		descriptors = registry.getPlugins();
	else {
		ArrayList list = new ArrayList(plugins.size());
		for (int i = 0; i < plugins.size(); i++) {
			PluginDescriptorModel descriptor = registry.getPlugin((String) plugins.get(i));
			if (descriptor != null)
				list.add(descriptor);
		}
		descriptors = (PluginDescriptorModel[]) list.toArray(new PluginDescriptorModel[list.size()]);
	}
	return descriptors;
}
protected List getDevEntries() {
	return devEntries;
}
protected String getInstall() {
	return install;
}
/**
 * convert a list of comma-separated tokens into an array
 */
protected List getListFromString(String prop) {
	if (prop == null || prop.trim().equals(""))
		return new ArrayList(0);
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(prop, ","); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return result;
}
private URL[] getPluginPath() {
	// get the plugin path.  If one was spec'd on the command line, use that.
	// Otherwise, if the install location was spec'd, compute the default path.
	// Finally, if nothing was said, allow the system to figure out the plugin
	// path based on the current running state.
	if (pluginPath == null && install != null) {
		try {
			return new URL[] { new URL("file:" + install + "/plugins/")};
		} catch (MalformedURLException e) {
		}
	} else {
		return BootLoader.getPluginPath(pluginPath);
	}
	return null;
}
protected Properties getProperties(InstallModel descriptor) {
	Properties result = (Properties)propertyValues.get(descriptor);
	if (result != null)
		return result;
	
	result = readProperties(new Path("file:" + descriptor.getLocation()).addTrailingSeparator().toString());
	propertyValues.put(descriptor,result);
	return result;
}
protected Properties getProperties(PluginModel descriptor) {
	Properties result = (Properties)propertyValues.get(descriptor);
	if (result != null)
		return result;

	result = readProperties(descriptor.getLocation());
	propertyValues.put(descriptor,result);
	return result;
}
protected PluginRegistryModel getRegistry() {
	return registry;
}
protected String getSubstitution(PluginModel descriptor,String propertyName) {
	return (String)getProperties(descriptor).get(propertyName);
}
protected String getSubstitution(InstallModel descriptor,String propertyName) {
	return (String)getProperties(descriptor).get(propertyName);
}
protected String makeRelative(String location, IPath base) {
	IPath path = new Path(location);
	if (!path.getDevice().equalsIgnoreCase(base.getDevice()))
		return location.toString();
	int baseCount = base.segmentCount();
	int count = base.matchingFirstSegments(path);
	if (count > 0) {
		String temp = "";
		for (int j = 0; j < baseCount - count; j++)
			temp += "../";
		path = new Path(temp).append(path.removeFirstSegments(count));
	}
	return path.toString();
}
/**
 * Print the usage of this launcher on the system console
 */
protected void printUsage(PrintWriter out) {
	out.println("The general form of using the VAJ Extractor is:");
	out.println("      java <launcher class> -application <name> [option list]");
	out.println("where the option list can be any number of the following:");
	out.println("      -? : print this message");
	out.flush();
}
protected String[] processCommandLine(String[] args) {
	for (int i = 0; i < args.length; i++) {
		// check for args without parameters (i.e., a flag arg)

		// look for the usage flag
		if (args[i].equals(USAGE)) {
			usage = true;
		}

		// check for args with parameters
		if (i == args.length - 1 || args[i + 1].startsWith("-")) {
			continue;
		}
		String arg = args[++i];

		// check for the plugin path arg
		if (args[i - 1].equalsIgnoreCase(PLUGINS))
			try {
				pluginPath = new URL(arg);
			} catch (MalformedURLException e) {
				try {
					pluginPath = new URL("file:" + arg);
				} catch (MalformedURLException e2) {
				}
			}

		// check for the install location arg
		if (args[i - 1].equalsIgnoreCase(INSTALL))
			install = arg;
			
					// set the additional development model class path entries
		if (args[i - 1].equalsIgnoreCase(DEV_ENTRIES))
			devEntries = getListFromString(arg);
	}
	return new String[0];
}
protected Properties readProperties(String modelDirectory) {
	Properties result = new Properties();
	
	try {
		URL propertiesFile = new URL(modelDirectory + FILENAME_PROPERTIES);
		InputStream is = propertiesFile.openStream();
		try {
			result.load(is);
		} finally {
			is.close();
		}
	} catch (IOException e) {
		// if the file does not exist then we'll use default values, which is fine
	}
	
	return result;
}
public Object run(Object args) throws Exception {
	processCommandLine((String[]) args);
	if (usage) {
		printUsage(new PrintWriter(System.out));
		return null;
	}
	URL[] path = getPluginPath();
	MultiStatus problems = new MultiStatus("vajextractor", 13, "plugin parsing problems", null);
	Factory factory = new Factory(problems);
	registry = Platform.parsePlugins(path, factory);
	return null;
}
protected String[] separateNameFromVersion(String name) {
	String result[] = new String[2];
	int lastSeparator = name.lastIndexOf(SEPARATOR_VERSION);
	if (lastSeparator == -1) {
		result[0] = name;
		result[1] = new String();
		return result;
	}
	
	String versionPortion = name.substring(lastSeparator + 1);
	try {
		new VersionIdentifier(versionPortion);
	} catch (NumberFormatException e) {
		result[0] = name;
		result[1] = new String();
		return result;
	}		
	
	result[0] = name.substring(0,lastSeparator);
	result[1] = versionPortion;
	
	return result;
}
public void setDevEntries(List value) {
	devEntries = value;
}
protected void setInstall(String value) {
	install = value;
}
protected void setRegistry(PluginRegistryModel value) {
	registry = value;
}
/**
 * Run this launcher with the arguments specified in the given string.
 * This is a short cut method for people running the launcher from
 * a scrapbook (i.e., swip-and-doit facility).
 */
public static String[] tokenizeArgs(String argString) throws Exception {
	Vector list = new Vector(5);
	for (StringTokenizer tokens = new StringTokenizer(argString, " "); tokens.hasMoreElements();)
		list.addElement((String) tokens.nextElement());
	return (String[]) list.toArray(new String[list.size()]);
}
}
