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
import org.eclipse.pde.internal.build.FetchScriptGeneratorApplication;
import org.eclipse.pde.internal.build.Utils;

/**
 * Wrapper class for the "eclipse.fetch" Ant task.
 */
public class FetchTask extends Task {

	/**
	 * The application associated with this Ant task.
	 */
	protected FetchScriptGeneratorApplication generator;

/**
 * Default constructor this class.
 */
public FetchTask() {
	generator = new FetchScriptGeneratorApplication();
}

/**
 * Set the boolean value indicating whether or not the fetch scripts should be
 * generated for the children of this feature.
 *  * @param children <code>true</code> if the children scripts should be generated
 *     and <code>false</code> otherwise */
public void setChildren(boolean children) {
	generator.setChildren(children);
}

/**
 * Set the location for the CVS password file.
 *  * @param cvsPassFileLocation the location of the password file */
public void setCvsPassFile(String cvsPassFileLocation) {
	generator.setCvsPassFile(cvsPassFileLocation);
}

/**
 *  * @param directoryLocation */
public void setDirectory(String directoryLocation) {
	generator.setDirectory(directoryLocation);
}

/**
 *  * @param elements */
public void setElements(String elements) {
	generator.setElements(Utils.getArrayFromString(elements));
}

/**
 *  * @param installLocation */
public void setInstall(String installLocation) {
	generator.setInstall(installLocation);
}

/**
 * @see org.apache.tools.ant.Task#execute() */
public void execute() throws BuildException {
	try {
		generator.run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

/**
 *  * @param scriptName */
public void setScriptName(String scriptName) {
	generator.setScriptName(scriptName);
}
}