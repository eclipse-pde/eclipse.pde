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

import java.util.*;

import org.eclipse.core.runtime.CoreException;

public class SourceBuildScriptGenerator {

	protected String sourceLocation;

public void run() throws CoreException {
	PluginModelSourceBuildScriptGenerator generator = new PluginModelSourceBuildScriptGenerator();
	generator.setSourceLocation(sourceLocation);
	generator.generate();
}

/**
 * Sets the pluginPath.
 */
public void setSourceLocation(String location) {
	this.sourceLocation = location;
}
}