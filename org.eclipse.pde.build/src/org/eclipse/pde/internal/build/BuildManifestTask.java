package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.model.ComponentModel;
import org.eclipse.core.runtime.model.ConfigurationModel;
import org.eclipse.core.runtime.model.PluginDescriptorModel;
import org.eclipse.core.runtime.model.PluginFragmentModel;
/**
 * Used to create a build manifest file describing
 * what plug-ins and versions were included in a
 * build.
 */
public class BuildManifestTask extends Task {
	protected boolean recursive = true;
	protected String[] elements;
	protected String destination;
	protected String directory;
	protected String build;
	protected Properties specs;
	protected PrintWriter output;

public BuildManifestTask() {
	super();
}

public void execute() throws BuildException {
	loadDirectory();
	initialize();
	do {
		for (int i = 0; i < elements.length; i++)
			generateEntry(elements[i]);
	} while (hasMoreElements(elements));
	output.close();
}
protected boolean hasMoreElements(String[] entries) {
	if (!recursive)
		return false;
	HashSet seen = new HashSet(10);
	ModelRegistry modelRegistry = new ModelRegistry();
	modelRegistry.seekComponents(destination);
	modelRegistry.seekConfigurations(destination);
	for (int i = 0; i < entries.length; i++) {
		int index = entries[i].indexOf('@');
		String type = entries[i].substring(0, index);
		String element = entries[i].substring(index + 1);
		if (type.equals("component")) {
			ComponentModel model = modelRegistry.getComponent(element);
			if (model == null)
				continue;
			PluginDescriptorModel[] plugins = model.getPlugins();
			for (int j = 0; j < plugins.length; j++) 
				seen.add("plugin@" + plugins[j].getId());
			PluginFragmentModel[] fragments = model.getFragments();
			for (int j = 0; j < fragments.length; j++) 
				seen.add("fragment@" + fragments[j].getId());
		}
		if (type.equals("configuration")) {
			ConfigurationModel model = modelRegistry.getConfiguration(element);
			if (model == null)
				continue;
			ComponentModel[] list = model.getComponents();
			for (int j = 0; j < list.length; j++)
				seen.add("component@" + list[j].getId());
		}
	}
	if (seen.size() == 0)
		return false;
	elements = (String[])seen.toArray(new String[seen.size()]);
	return true;
}

protected void initialize() throws BuildException {
	try {
		output = new PrintWriter(new FileOutputStream(destination + "/" + "buildmanifest.properties"));
	} catch (FileNotFoundException e) {
		throw new BuildException(e.getMessage());
	}
	output.print("# Build Manifest for ");
	output.println(build);
	output.println();
	output.println("# The format of this file is:");
	output.println("# <element>@=<version>");
	output.println();
}
protected void generateEntry(String entry) {
	String spec = (String) specs.get(entry);
	if (spec == null)
		return;
	String[] specFields = PluginTool.getArrayFromString(spec);
	int index = entry.indexOf('@');
	String type = entry.substring(0, index);
	if (!type.equals("plugin") && !type.equals("fragment"))
		return;
	output.print(entry);
	output.print("=");
	output.println(specFields[0]);
}
protected void loadDirectory() {
	specs = new Properties();
	try {
		InputStream is = new FileInputStream(directory);
		try {
			specs.load(is);
		} finally {
			is.close();
		}
	} catch (IOException e) {
		// if the file does not exist then we'll use default values, which is fine
	}
}

public void setDirectory(String value) {
	directory = value;
}

public void setElements(String value) {
	elements = PluginTool.getArrayFromString(value);
}

public void setDestination(String value) {
	destination = value;
}

public void setRecursive(boolean value) {
	recursive = value;
}

public void setBuild(String value) {
	build = value;
}
}