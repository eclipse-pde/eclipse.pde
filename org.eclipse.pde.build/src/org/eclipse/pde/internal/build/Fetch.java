package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Commandline;
import org.eclipse.ant.core.EclipseProject;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.model.*;

public class Fetch extends PluginTool {
	String[] elements;
	String directory = "directory.txt";
	Properties specs;
	Project parent;
	String passfile = null;
	boolean recursive = true;
	boolean children = false;
	boolean componentsFound = false;
	boolean configurationsFound = false;
	boolean pluginsFound = false;
	boolean fragmentsFound = false;
	
void addChildren() {
	HashSet seen = new HashSet(10);
	ModelRegistry modelRegistry = new ModelRegistry();
	modelRegistry.seekComponents(getInstall());
	modelRegistry.seekConfigurations(getInstall());
	for (int i = 0; i < elements.length; i++) {
		int index = elements[i].indexOf('@');
		String type = elements[i].substring(0, index);
		String element = elements[i].substring(index + 1);
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
	elements = (String[])seen.toArray(new String[seen.size()]);
}
void addMkdir(Target target, String location) {
	Mkdir task = new Mkdir();
	task.setProject(target.getProject());
	task.setDir(new File(location));
	target.addTask(task);
}
public void execute() throws Exception {
	if (children)
		addChildren();
	Project project = generateFetchScript();
	project.executeTarget("fetch");
	if (recursive && (configurationsFound || componentsFound))
		fetchNextLevel();	
}

private void fetchNextLevel() throws Exception {
	ArrayList args = new ArrayList(10);
	args.add("-children");
	args.add("-install");
	args.add(getInstall());
	args.add("-directory");
	args.add(directory);
	if (recursive)
		args.add("-recursive");	
	args.add("-elements");
	args.add(getStringFromCollection(Arrays.asList(elements), "", "", ","));
	new Fetch().run((String[])args.toArray(new String[args.size()]));
}
protected void generateEpilogue(Project output) {
//	output.println("</project>");
}
protected void generateFetchEntry(Target output, String entry) {
	String spec = (String)specs.get(entry);
	if (spec == null)
		return;
	String[] specFields = getArrayFromString(spec);
	int index = entry.indexOf('@');
	String type = entry.substring(0, index);
	String element = entry.substring(index + 1);
	String location = getInstall() + "/";
	if (type.equals("component")) {
		location += "install/" + type + "s";
		componentsFound = true;
	} else {
		if (type.equals("configuration")) {
			location += "install/" + type + "s";
			configurationsFound = true;
		} else {
			if (type.equals("plugin")) {
				location += type + "s";
				pluginsFound = true;
			} else {
				if (type.equals("plugin")) {
					location += type + "s";
					pluginsFound = true;
				}
			}
		}
	}
	// <cvspass cvsroot=":pserver:<user>@<host>:<repo>" password="abc123" [passfile="<file location>]"/>
	boolean needLogout = false;
	if (specFields.length == 3 && specFields[2].length() > 0) {
		needLogout = true;
		CVSPass pass = new CVSPass();
		pass.setCvsroot(specFields[1]);
		pass.setPassword(specFields[2]);
		if (BootLoader.getOS().equals(BootLoader.OS_WIN32))
			pass.setPassfile(new File("c:/.cvspass"));
		pass.setProject(output.getProject());
		if (passfile != null)
			pass.setPassfile(new File(passfile));
		output.addTask(pass);
	}

	Echo echo = new Echo();
	echo.setProject(output.getProject());
	echo.setMessage("Fetching: " + element + " from " + specFields[1]);
	output.addTask(echo);

	//	<exec dir="./<type location>" executable="cvsexe">
	//	  <arg line="-d :pserver:<user>@<host>:<repo> checkout -r <label> <module>"/>
	//	</exec>
	Cvs cvs = new Cvs();
	cvs.setProject(output.getProject());
	cvs.setCommand("checkout");
	cvs.setCvsRoot(specFields[1]);
	cvs.setTag(specFields[0]);
	cvs.setPackage(element);
	cvs.setQuiet(true);
	cvs.setOutput(new File("cvs.log"));
	if (BootLoader.getOS().equals(BootLoader.OS_WIN32))
		cvs.setPassfile(new File("c:/.cvspass"));
	cvs.setDest(new File(location));
	output.addTask(cvs);
	
	//	<exec dir="." executable="cvsexe">
	//	  <arg line="-d :pserver:<user>@<host>:<repo> logout"/>
	//	</exec>
	if (needLogout) {
		cvs = new Cvs();
		cvs.setProject(output.getProject());
		cvs.setCommand("logout");
		cvs.setCvsRoot(specFields[1]);
		if (BootLoader.getOS().equals(BootLoader.OS_WIN32))
			cvs.setPassfile(new File("c:/.cvspass"));
		output.addTask(cvs);
	}
}
public Project generateFetchScript() {
	Project project = new EclipseProject();
	initialize();
	generatePrologue(project);
	generateFetchTarget(project);
	generateMkdirsTarget(project);
	generateEpilogue(project);
	if (parent != null) {
		Vector listeners = parent.getBuildListeners();
		for (int i = 0; i < listeners.size(); i++)
			project.addBuildListener((BuildListener)listeners.elementAt(i));
	}
	project.init();
	return project;
}
protected void generateFetchTarget(Project output) {
//	output.println("<target name=\"fetch\">");
	Target target = new Target();
	target.setName("fetch");
	target.setDepends("mkdirs");
	output.addTarget(target);
	for (int i = 0; i < elements.length; i++)
		generateFetchEntry(target, elements[i]);
//	output.println("</target>");
}
protected void generateMkdirsTarget(Project output) {
	Target target = new Target();
	target.setName("mkdirs");
	output.addTarget(target);
	if (configurationsFound) 
		addMkdir(target, getInstall() + "/install/configurations");
	if (componentsFound) 
		addMkdir(target, getInstall() + "/install/components");
	if (pluginsFound) 
		addMkdir(target, getInstall() + "/plugins");
	if (fragmentsFound) 
		addMkdir(target, getInstall() + "/fragments");
}
protected void generatePrologue(Project output) {
	// output.println("<project name=\"Fetch\" basedir=\".\" default=\"all\">");
	output.setName("Fetch");
	output.setBaseDir(new File("."));
	output.setDefault("fetch");
}
protected void initialize() {
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
public Object run(Object args) throws Exception {
	super.run(args);
	execute();
	return null;
}
public Object run(Object args, Project parent) throws Exception {
	super.run(args);
	this.parent = parent;
	execute();
	return null;
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
		if (args[i].equalsIgnoreCase("-recursive")) {
			recursive = true;
			continue;
		}
		// check for args with parameters
		if (i == args.length - 1 || args[i + 1].startsWith("-")) 
			continue;
		String arg = args[++i];

		if (args[i - 1].equalsIgnoreCase("-elements"))
			elements = getArrayFromString(arg);

		if (args[i - 1].equalsIgnoreCase("-directory"))
			directory = arg;

		if (args[i - 1].equalsIgnoreCase("-passfile"))
			passfile = arg;
	}
	return null;
}
}

