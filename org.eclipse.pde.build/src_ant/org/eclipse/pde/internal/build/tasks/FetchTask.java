/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.pde.internal.build.FetchScriptGenerator;
import org.eclipse.pde.internal.build.builder.AbstractBuildScriptGenerator;

/**
 * Wrapper class for the "eclipse.fetch" Ant task.
 */
public class FetchTask extends Task {

	/**
	 * The application associated with this Ant task.
	 */
	protected FetchScriptGenerator generator;

	/**
	 * Default constructor this class.
	 */
	public FetchTask() {
		generator = new FetchScriptGenerator();
	}

	/**
	 * Set the boolean value indicating whether or not the fetch scripts should be
	 * generated for the children of this feature.
	 * 
	 * @param children <code>true</code> if the children scripts should be generated
	 *     and <code>false</code> otherwise
	 */
	public void setChildren(boolean children) {
		generator.setFetchChildren(children);
	}

	/**
	 * Set the location for the CVS password file.
	 * 
	 * @param cvsPassFileLocation the location of the password file
	 */
	public void setCvsPassFile(String cvsPassFileLocation) {
		generator.setCvsPassFileLocation(cvsPassFileLocation);
	}

	/**
	 * 
	 * @param directoryLocation
	 */
	public void setDirectory(String directoryLocation) {
		generator.setDirectoryLocation(directoryLocation);
	}

	/**
	 * 
	 * @param elements
	 */
	public void setElements(String element) {
		try {
			generator.setElement(element);
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	public void setFetchTag(String value) {
		generator.setFetchTag(value);
	}

	/**
	 * 
	 * @param installLocation
	 */
	public void setBuildDirectory(String installLocation) {
		generator.setWorkingDirectory(installLocation);
	}

	/**
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			generator.generate();
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	public void setRecursiveGeneration(boolean recursiveGeneration) {
		generator.setRecursiveGeneration(recursiveGeneration);
	}

	public void setConfigInfo(String configInfo) throws CoreException {
		AbstractBuildScriptGenerator.setConfigInfo(configInfo);
	}

}