/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
 * Wrapper class for an Ant task which generates the build scripts.
 */
public class BuildScriptGeneratorTask extends Task implements IXMLConstants, IPDEBuildConstants {

	/**
	 * The application associated with this Ant task.
	 */
	protected BuildScriptGenerator generator = new BuildScriptGenerator();

/**
 * Set the boolean value indicating whether or not children scripts should
 * be generated.
 *  * @param children <code>true</code> if child scripts should be generated
 *     and <code>false</code> otherwise */
public void setChildren(boolean children) {
	generator.setChildren(children);
}

/**
 * Set the development entries for the compile classpath to be the given
 * value.
 *  * @param devEntries the classpath dev entries */
public void setDevEntries(String devEntries) {
	generator.setDevEntries(Utils.getArrayFromString(devEntries));
}

/**
 * Set the plug-in path to be the given value.
 *  * @param pluginPath the plug-in path */
public void setPluginPath(String pluginPath) {
	generator.setPluginPath(Utils.getArrayFromString(pluginPath));
}

/**
 * Set the source elements for the script to be the given value.
 *  * @param elements the source elements for the script */
public void setElements(String elements) {
	generator.setElements(Utils.getArrayFromString(elements));
}

/**
 * @see org.apache.tools.ant.Task#execute() */
public void execute() throws BuildException {
	try {
		run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

/**
 * Execute the script generator.
 *  * @throws CoreException if there was a problem generating the script */
public void run() throws CoreException {
	generator.run();
}

/**
 * Set the install location to be the given value.
 *  * @param installLocation the install location */
public void setInstall(String installLocation) {
	generator.setInstall(installLocation);
}

}