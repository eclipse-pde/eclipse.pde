package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.tasks.BuildScriptGeneratorTask;

/**
 * Application object that dispatches script generation calls to feature,
 * plug-in and fragment generators.
 */
public class BuildScriptGeneratorApplication extends AbstractApplication {

	/**
	 * Where to find the elements.
	 */
	protected BuildScriptGeneratorTask task;

/**
 * 
 */
public BuildScriptGeneratorApplication() {
	task = new BuildScriptGeneratorTask();
}

/**
 * 
 */
public void run() throws CoreException {
	task.run();
}

/**
 * @see AbstractApplication#processCommandLine
 */
protected void processCommandLine(List commands) {
	super.processCommandLine(commands);

	// looks for flag-like commands
	if (commands.remove(ARG_NO_CHILDREN)) 
		task.setChildren(false);

	// looks for param/arg-like commands
	task.internalSetElements(getArguments(commands, ARG_ELEMENTS));
	task.internalSetDevEntries(getArguments(commands, ARG_DEV_ENTRIES));
	task.internalSetPlugins(getArguments(commands, ARG_PLUGIN_PATH));
	String[] arguments = getArguments(commands, ARG_INSTALL_LOCATION);
	task.setInstall(arguments[0]); // only consider one location
	arguments = getArguments(commands, ARG_OS);
	if (arguments != null)
		task.setOS(arguments[0]); // only consider one value
	arguments = getArguments(commands, ARG_WS);
	if (arguments != null)
		task.setWS(arguments[0]); // only consider one value
	arguments = getArguments(commands, ARG_NL);
	if (arguments != null)
		task.setNL(arguments[0]); // only consider one value
	arguments = getArguments(commands, ARG_ARCH);
	if (arguments != null)
		task.setARCH(arguments[0]); // only consider one value
}


protected void printUsage(PrintWriter out) {
	super.printUsage(out);
	out.println(ARG_NO_CHILDREN);
	out.print("\t\t bla bla bla");
	out.println(ARG_INSTALL_LOCATION);
	out.print("\t\t bla bla bla");
}

}