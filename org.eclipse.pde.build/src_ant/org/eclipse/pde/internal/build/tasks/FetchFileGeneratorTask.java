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
import org.eclipse.pde.internal.build.packager.FetchFileGenerator;

public class FetchFileGeneratorTask extends Task {
	protected FetchFileGenerator fileFetcher = new FetchFileGenerator();

	public void setWorkingDirectory(String workingDirectory) {
		fileFetcher.setWorkingDirectory(workingDirectory);
	}

	public void setConfigInfo(String config) throws CoreException {
		FetchFileGenerator.setConfigInfo(config);
	}

	public void setContentFilter(String filter) {
		fileFetcher.setContentFilter(filter);
	}

	public void setMap(String mapLocation) {
		fileFetcher.setMapLocation(mapLocation);
	}

	public void execute() throws BuildException {
		try {
			fileFetcher.generate();
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}
}
