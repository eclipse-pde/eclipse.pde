package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Commandline.Argument;
import org.eclipse.ant.core.EclipseProject;
import org.eclipse.core.runtime.model.*;

public class Fetch extends PluginTool {
	String[] elements;
	String directory = "directory.txt";
	Properties specs;
	String cvsexe;
	String passfile = null;
	boolean recursive = true;
	boolean children = false;
	boolean moreLevels = false;
	
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
public void execute() throws Exception {
	if (children)
		addChildren();
	Project project = generateFetchScript();
	project.executeTarget("fetch");
	if (recursive && moreLevels)
		fetchNextLevel();	
}

private void fetchNextLevel() throws Exception {
	ArrayList args = new ArrayList(10);
	args.add("-children");
	args.add("-install");
	args.add(getInstall());
	args.add("-directory");
	args.add(directory);
	args.add("-cvs");
	args.add(cvsexe);
	if (recursive)
		args.add("-recursive");	
	args.add("-elements");
	args.add(getStringFromCollection(Arrays.asList(elements), "", "", ","));
	new Fetch().run((String[])args.toArray(new String[args.size()]));
}
public Project generateFetchScript() {
	Project project = new EclipseProject();
	initialize();
	generatePrologue(project);
	generateFetchEntries(project);
//	generateCleanTarget(output);
	generateEpilogue(project);
	return project;
}
protected void generateFetchEntries(Project output) {
//	output.println("<target name=\"fetch\">");
	Target target = new Target();
	target.setName("fetch");
	output.addTarget(target);
	for (int i = 0; i < elements.length; i++)
		generateFetchEntry(target, elements[i]);
//	output.println("</target>");
}
protected void generateEpilogue(Project output) {
//	output.println("</project>");
}
protected void generateFetchEntry(Target output, String entry) {
System.out.println("fetch " + entry);
	String spec = (String)specs.get(entry);
	if (spec == null)
		return;
	String[] specFields = getArrayFromString(spec);
	int index = entry.indexOf('@');
	String type = entry.substring(0, index);
	String element = entry.substring(index + 1);
	if (type.equals("component") || type.equals("configuration")) {
		type = "install/" + type;
		moreLevels = true;
	}
	// <cvspass cvsroot=":pserver:<user>@<host>:<repo>" password="abc123" [passfile="<file location>"/>
	CVSPass pass = new CVSPass();
	pass.setCvsroot(specFields[1]);
	pass.setPassword(specFields[2]);
	pass.setPassfile(new File("c:/.cvspass"));
	pass.setProject(output.getProject());
	if (passfile != null)
		pass.setPassfile(new File(passfile));
	output.addTask(pass);

	//	<exec dir="./<type location>" executable="cvsexe">
	//	  <arg line="-d :pserver:<user>@<host>:<repo> checkout -r <label> <module>"/>
	//	</exec>
	ExecTask exec = new ExecTask();
	exec.setDir(new File(getInstall() + "/" + type + "s"));
	exec.setExecutable(cvsexe);
	exec.setProject(output.getProject());
	Commandline.Argument arg = exec.createArg();
	arg.setLine("-d " + specFields[1] + " checkout -r " + specFields[0] + " " + element);
	output.addTask(exec);

	//	<exec dir="." executable="cvsexe">
	//	  <arg line="-d :pserver:<user>@<host>:<repo> logout"/>
	//	</exec>
	exec = new ExecTask();
	exec.setDir(new File("."));
	exec.setExecutable(cvsexe);
	exec.setProject(output.getProject());
	arg = exec.createArg();
	arg.setLine("-d " + specFields[1] + " logout/>");
	output.addTask(exec);
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
System.out.println(new File(directory).getAbsolutePath());
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

		if (args[i - 1].equalsIgnoreCase("-cvs"))
			cvsexe = arg;

		if (args[i - 1].equalsIgnoreCase("-directory"))
			directory = arg;

		if (args[i - 1].equalsIgnoreCase("-passfile"))
			passfile = arg;
	}
	return null;
}
}

