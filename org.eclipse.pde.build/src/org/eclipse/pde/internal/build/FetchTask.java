package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class FetchTask extends Task {
	boolean recursive = true;
	boolean children = false;
	String elements;
	String install;
	String directory;
	String cvs;

public void execute() throws BuildException {
	try {
		ArrayList args = new ArrayList(10);
		args.add("-install");
		args.add(install);
		args.add("-elements");
		args.add(elements);
		args.add("-directory");
		args.add(directory);
		if (children)
			args.add("-children");
		if (recursive)
			args.add("-recursive");
		args.add("-cvs");
		args.add(cvs);
		String[] argArray = (String[])args.toArray(new String[args.size()]);
		new Fetch().run(argArray);
	} catch (Exception e) {
		throw new BuildException("Unable to generate fetch.xml", e);
	}
}
public void setChildren(boolean value) {
	children = value;
}
public void setCVS(String value) {
	cvs = value;
}
public void setDirectory(String value) {
	directory = value;
}
public void setElements(String value) {
	elements = value;
}
public void setInstall(String value) {
	install = value;
}
public void setRecursive(boolean value) {
	recursive = value;
}
}
