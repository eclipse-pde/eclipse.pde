package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.tasks.FetchTask;
/**
 * 
 */
public class FetchScriptGeneratorApplication extends AbstractApplication {

	/**
	 * 
	 */
	protected FetchTask task;

public FetchScriptGeneratorApplication() {
	task = new FetchTask();
}

protected void run() throws CoreException {
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
	String[] arguments = getArguments(commands, ARG_INSTALL_LOCATION);
	task.setInstall(arguments[0]); // only consider one location
	arguments = getArguments(commands, ARG_DIRECTORY_LOCATION);
	task.setDirectory(arguments[0]); // only consider one location
	arguments = getArguments(commands, ARG_CVS_PASSFILE_LOCATION);
	if (arguments != null)
		task.setCvsPassFile(arguments[0]); // only consider one location
	arguments = getArguments(commands, ARG_SCRIPT_NAME);
	if (arguments != null)
		task.setScriptName(arguments[0]); // only consider one name
}
}