/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

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
 * Default constructor for the class. 
 */
public BuildScriptGeneratorApplication() {
	generator = new BuildScriptGenerator();
}

/**
 * 
 * @see AbstractApplication#run()
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
}
