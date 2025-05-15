/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.packager.FetchFileGenerator;

/**
 * Internal Task.
 * Generate fetch script to get files from a URL based on a packager map.
 * @since 3.0
 */
public class FetchFileGeneratorTask extends Task {
	protected FetchFileGenerator fileFetcher = new FetchFileGenerator();

	/**
	 * Set the folder in which the scripts will be generated.
	 * @param workingDirectory the location where the scripts will be generated.
	 */
	public void setWorkingDirectory(String workingDirectory) {
		fileFetcher.setWorkingDirectory(workingDirectory);
	}

	/**
	 * Set the configuration for which the script should be generated. The default is set to be configuration independent.
	 * @param config an ampersand separated list of configuration (for example {@code win32, win32, x86 & macoxs, carbon, ppc}).
	 */
	public void setConfigInfo(String config) throws CoreException {
		AbstractScriptGenerator.setConfigInfo(config);
	}

	/**
	 * Set the filters used to select the content type of the components to be downloaded. The values are matched against values from the map.
	 * @param filter a comma separated list of content.
	 */
	public void setContentFilter(String filter) {
		fileFetcher.setContentFilter(filter);
	}

	/**
	 * Set the filters used to select the components to be downloaded. The values are matched against values from the map.
	 * @param components a comma separated list of components.
	 */
	public void setComponentFilter(String components) {
		fileFetcher.setComponentFilter(components);
	}

	/**
	 * Set the path the a packager map file.
	 * @param mapLocation path the a packager map file.
	 */
	public void setMap(String mapLocation) {
		fileFetcher.setMapLocation(mapLocation);
	}

	@Override
	public void execute() throws BuildException {
		try {
			BundleHelper.getDefault().setLog(this);
			fileFetcher.generate();
			BundleHelper.getDefault().setLog(null);
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus()), e);
		}
	}
}
