package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ScriptBuilder extends PluginTool {
	String[] elements;
	boolean children = true;
		
public void execute() throws Exception {
	List[] types = sortElements();
	if (!types[0].isEmpty())
		new PluginBuildScriptGenerator().run(new String[] {"-install", getInstall(), "-elements", getStringFromCollection(types[0], "", "", ",")});
	if (!types[1].isEmpty())
		new FragmentBuildScriptGenerator().run(new String[] {"-install", getInstall(), "-elements", getStringFromCollection(types[1], "", "", ",")});
	if (!types[2].isEmpty()) {
		List components = types[2];
		for (int i = 0; i < components.size(); i++)
			new ComponentBuildScriptGenerator().run(new String[] {"-install", getInstall(), "-elements", (String)components.get(i), children ? "" : "-noChildren"});
	}
	if (!types[3].isEmpty()) {
		List configurations = types[3];
		for (int i = 0; i < configurations.size(); i++)
			new ConfigurationBuildScriptGenerator().run(new String[] {"-install", getInstall(), "-elements", (String)configurations.get(i), children ? "" : "-noChildren"});
	}
}
protected void printUsage(PrintWriter out) {
}
protected String[] processCommandLine(String[] args) {
	super.processCommandLine(args);
	for (int i = 0; i < args.length; i++) {
		// check for args without parameters (i.e., a flag arg)
		if (args[i].equalsIgnoreCase("-children")) {
			children = true;
			continue;
		}
		// check for args with parameters
		if (i == args.length - 1 || args[i + 1].startsWith("-")) 
			continue;
		String arg = args[++i];

		if (args[i - 1].equalsIgnoreCase("-elements"))
			elements = getArrayFromString(arg);
	}
	return null;
}
public Object run(Object args) throws Exception {
	super.run(args);
	execute();
	return null;
}
protected List[] sortElements() {
	List plugins = new ArrayList(5);
	List fragments = new ArrayList(5);
	List components = new ArrayList(5);
	List configurations = new ArrayList(5);
	for (int i = 0; i < elements.length; i++) {
		int index = elements[i].indexOf('@');
		String type = elements[i].substring(0, index);
		String element = elements[i].substring(index + 1);
		if (type.equals("plugin")) 
			plugins.add(element);
		if (type.equals("fragment")) 
			fragments.add(element);
		if (type.equals("component")) 
			components.add(element);
		if (type.equals("configuration")) 
			configurations.add(element);
	}
	return new List[] {plugins, fragments, components, configurations};
}
}
