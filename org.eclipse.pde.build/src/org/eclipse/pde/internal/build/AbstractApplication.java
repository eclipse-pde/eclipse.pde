package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.CoreException;

/**
 * Defines common behaviour for PDE Core applications.
 */
public abstract class AbstractApplication implements IPlatformRunnable, IPDEBuildConstants {

/**
 * Starting point for application logic.
 */
protected abstract void run() throws CoreException;

/*
 * @see IPlatformRunnable#run(Object)
 */
public Object run(Object args) throws Exception {
	processCommandLine(Utils.getArrayList((String[]) args));
	try {
		run();
	} catch (CoreException e) {
		e.printStackTrace(System.out);
	}
	return null;
}



/**
 * Looks for interesting command line arguments.
 */
protected void processCommandLine(List commands) {
}

/**
 * From a command line list, get the array of arguments of a given parameter.
 * The parameter and its arguments are removed from the list.
 * @return null if the parameter is not found or has no arguments
 */
protected String[] getArguments(List commands, String param) {
	int index = commands.indexOf(param);
	if (index == -1)
		return null;
	commands.remove(index);
	if (index == commands.size()) // if this is the last command
		return null;
	List args = new ArrayList(commands.size());
	while (index < commands.size()) { // while not the last command
		String command = (String) commands.get(index);
		if (command.startsWith("-")) // is it a new parameter?
			break;
		args.add(command);
		commands.remove(index);
	}
	if (args.isEmpty())
		return null;
	return (String[]) args.toArray(new String[args.size()]);
}




}
