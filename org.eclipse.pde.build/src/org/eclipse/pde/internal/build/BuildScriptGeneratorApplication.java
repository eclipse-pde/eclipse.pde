package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
 * Application object that dispatches script generation calls to feature,
 * plug-in and fragment generators.
 */
public class BuildScriptGeneratorApplication extends AbstractApplication {

	/**
	 * Where to find the elements.
	 */
	protected BuildScriptGenerator generator;

/**
 * 
 */
public BuildScriptGeneratorApplication() {
	generator = new BuildScriptGenerator();
}

/**
 * 
 */
public void run() throws CoreException {
	generator.run();
}

/**
 * @see AbstractApplication#processCommandLine
 */
protected void processCommandLine(List commands) {
	super.processCommandLine(commands);

	// looks for flag-like commands
	if (commands.remove(ARG_NO_CHILDREN)) 
		generator.setChildren(false);

	// looks for param/arg-like commands
	generator.setElements(getArguments(commands, ARG_ELEMENTS));
	generator.setDevEntries(getArguments(commands, ARG_DEV_ENTRIES));
	generator.setPlugins(getArguments(commands, ARG_PLUGIN_PATH));
	String[] arguments = getArguments(commands, ARG_INSTALL_LOCATION);
	generator.setInstall(arguments[0]); // only consider one location
}

protected void printUsage(PrintWriter out) {
	super.printUsage(out);
	out.println(ARG_NO_CHILDREN);
	out.print("\t\t bla bla bla");
	out.println(ARG_INSTALL_LOCATION);
	out.print("\t\t bla bla bla");
}

}