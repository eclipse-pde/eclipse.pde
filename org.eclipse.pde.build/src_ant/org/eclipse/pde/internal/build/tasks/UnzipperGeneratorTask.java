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

import java.net.MalformedURLException;
import org.apache.tools.ant.BuildException;
import org.eclipse.ant.core.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.packager.UnzipperGenerator;

public class UnzipperGeneratorTask extends Task {
	private UnzipperGenerator generator = new UnzipperGenerator();

	public void execute() throws BuildException {
		try {
			generator.generate();
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	public void setZipsDirectory(String filename) {
		generator.setDirectoryLocation(filename);
	}

	//FIXME To rename
	public void setWorkingDirectory(String installLocation) throws MalformedURLException {
		generator.setWorkingDirectory(installLocation);
	}

	public void setConfigInfo(String configInfo) throws BuildException {
		try {
			AbstractScriptGenerator.setConfigInfo(configInfo);
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	/**
	 *  Set the property file containing information about packaging
	 * @param propertyFile: the path to a property file
	 */
	public void setPackagePropertyFile(String propertyFile) {
		generator.setPropertyFile(propertyFile);
	}
}
