/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;
/**
 * 
 */
public class BuildScriptGeneratorTask extends Task implements IXMLConstants, IPDEBuildConstants {

	/**
	 * 
	 */
	protected BuildScriptGenerator generator = new BuildScriptGenerator();

	/**
	 * Build variables.
	 */
	protected String buildVariableOS;
	protected String buildVariableWS;
	protected String buildVariableNL;
	protected String buildVariableARCH;

/**
 * Sets the children.
 */
public void setChildren(boolean children) {
	generator.setChildren(children);
}


/**
 * Sets the devEntries.
 */
public void setDevEntries(String devEntries) {
	generator.setDevEntries(Utils.getArrayFromString(devEntries));
}

/**
 * Sets the pluginPath.
 */
public void setPluginPath(String pluginPath) {
	generator.setPluginPath(Utils.getArrayFromString(pluginPath));
}






/**
 * Sets the elements.
 */
public void setElements(String elements) {
	generator.setElements(Utils.getArrayFromString(elements));
}





public void execute() throws BuildException {
	try {
		run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

public void run() throws CoreException {
	generator.run();
}









/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	generator.setInstall(installLocation);
}

/**
 * Sets the buildVariableARCH.
 */
public void setARCH(String buildVariableARCH) {
	this.buildVariableARCH = buildVariableARCH;
}

/**
 * Sets the buildVariableNL.
 */
public void setNL(String buildVariableNL) {
	this.buildVariableNL = buildVariableNL;
}

/**
 * Sets the buildVariableOS.
 */
public void setOS(String buildVariableOS) {
	this.buildVariableOS = buildVariableOS;
}

/**
 * Sets the buildVariableWS.
 */
public void setWS(String buildVariableWS) {
	this.buildVariableWS = buildVariableWS;
}

}