package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ScriptBuilderTask extends Task {
	boolean children = false;
	String elements;
	String install;

public void execute() throws BuildException {
	try {
		ArrayList args = new ArrayList(10);
		args.add("-install");
		args.add(install);
		args.add("-elements");
		args.add(elements);
		if (children)
			args.add("-children");
		String[] argArray = (String[])args.toArray(new String[args.size()]);
		new ScriptBuilder().run(argArray);
	} catch (Exception e) {
		throw new BuildException("Unable to generate fetch.xml", e);
	}
}
public void setChildren(boolean value) {
	children = value;
}
public void setElements(String value) {
	elements = value;
}
public void setInstall(String value) {
	install = value;
}
}
