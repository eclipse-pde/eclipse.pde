/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.apache.tools.ant.BuildException;
import org.eclipse.ant.core.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.packager.UnzipperGenerator;

/**
 * Internal Task.
 * This task generates an unzipper script that unzip a files.
 * @since 3.0
 */
public class UnzipperGeneratorTask extends Task {
	private UnzipperGenerator generator = new UnzipperGenerator();

	public void execute() throws BuildException {
		try {
			BundleHelper.getDefault().setLog(this);
			generator.generate();
			BundleHelper.getDefault().setLog(null);
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus(), null).toString());
		}
	}

	/**
	 * Set the name of the file listing all the files that must be unzipped.
	 * @param filename
	 */
	public void setZipsDirectory(String filename) {
		generator.setDirectoryLocation(filename);
	}

	/**
	 * Set the folder in which the scripts will be generated.
	 * @param installLocation the location where the scripts will be generated and the files fetched.
	 */
	public void setWorkingDirectory(String installLocation) {
		generator.setWorkingDirectory(installLocation);
	}

	/** 
	 * Set the configuration for which the script should be generated. The default is set to be configuration independent.
	 * @param configInfo an ampersand separated list of configuration (for example win32, win32, x86 & macoxs, carbon, ppc).
	 * @throws BuildException
	 */
	public void setConfigInfo(String configInfo) throws BuildException {
		try {
			AbstractScriptGenerator.setConfigInfo(configInfo);
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}

	/**
	 *  Set the property file containing information about packaging
	 * @param propertyFile the path to a property file
	 */
	public void setPackagePropertyFile(String propertyFile) {
		generator.setPropertyFile(propertyFile);
	}
}
