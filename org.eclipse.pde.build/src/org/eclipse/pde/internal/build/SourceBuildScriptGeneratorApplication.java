/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
 *
 */
public class SourceBuildScriptGeneratorApplication extends AbstractApplication {

	protected SourceBuildScriptGenerator generator;

public SourceBuildScriptGeneratorApplication() {
	generator = new SourceBuildScriptGenerator();
}

/**
 * @see AbstractApplication#run()
 */
protected void run() throws CoreException {
	generator.run();
}

/**
 * @see AbstractApplication#processCommandLine
 */
protected void processCommandLine(List commands) {
	super.processCommandLine(commands);

	// looks for param/arg-like commands
	String[] arguments = getArguments(commands, ARG_SOURCE_LOCATION);
	generator.setSourceLocation(arguments[0]); // only consider one location
}
}