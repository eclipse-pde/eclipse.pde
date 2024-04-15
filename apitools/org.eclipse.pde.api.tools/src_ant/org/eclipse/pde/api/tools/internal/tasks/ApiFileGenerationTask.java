/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import org.apache.tools.ant.Task;
import org.eclipse.pde.api.tools.internal.APIFileGenerator;

/**
 * Ant task to generate the .api_description file during the Eclipse build.
 */
public class ApiFileGenerationTask extends Task/* APIFileGenerator */ {

	private final APIFileGenerator apiFileGenerator;

	public ApiFileGenerationTask() {
		this.apiFileGenerator = new APIFileGenerator();
	}

	/**
	 * Set the project name.
	 *
	 * @param projectName the given project name
	 */
	public void setProjectName(String projectName) {
		apiFileGenerator.projectName = projectName;
	}

	/**
	 * Set the project location.
	 *
	 * <br>
	 * <br>
	 * This is the folder that contains all the source files for the given project.
	 * <br>
	 * <br>
	 * The location is set using an absolute path.
	 *
	 * @param projectLocation the given project location
	 */
	public void setProject(String projectLocation) {
		apiFileGenerator.projectLocation = projectLocation;
	}

	/**
	 * Set the target location.
	 *
	 * <br>
	 * <br>
	 * This is the folder in which the generated files are generated. <br>
	 * <br>
	 * The location is set using an absolute path.
	 *
	 * @param targetLocation the given target location
	 */
	public void setTarget(String targetLocation) {
		apiFileGenerator.targetFolder = targetLocation;
	}

	/**
	 * Set the binary locations.
	 *
	 * <br>
	 * <br>
	 * This is a list of folders or jar files that contain all the .class files
	 * for the given project. They are separated by the platform path separator.
	 * Each entry must exist. <br>
	 * <br>
	 * They should be specified using absolute paths.
	 *
	 * @param binaryLocations the given binary locations
	 */
	public void setBinary(String binaryLocations) {
		apiFileGenerator.binaryLocations = binaryLocations;
	}

	/**
	 * Set if the task should scan the project even if it is not API tools
	 * enabled.
	 * <p>
	 * The possible values are: <code>true</code> or <code>false</code>
	 * </p>
	 * <p>
	 * Default is: <code>false</code>.
	 * </p>
	 *
	 * @since 1.2
	 */
	public void setAllowNonApiProject(String allow) {
		apiFileGenerator.allowNonApiProject = Boolean.parseBoolean(allow);
	}

	/**
	 * Sets the encoding the task should use when reading text streams
	 *
	 * @since 1.0.600
	 */
	public void setEncoding(String encoding) {
		apiFileGenerator.encoding = encoding;
	}

	/**
	 * Set the debug value.
	 * <p>
	 * The possible values are: <code>true</code>, <code>false</code>
	 * </p>
	 * <p>
	 * Default is <code>false</code>.
	 * </p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		apiFileGenerator.debug = Boolean.toString(true).equals(debugValue);
	}

	/**
	 * Set the extra manifest files' locations.
	 *
	 * <p>
	 * This is a list of extra MANIFEST.MF files' locations that can be set to
	 * provide more API packages to scan. They are separated by the platform
	 * path separator. Each entry must exist.
	 * </p>
	 * <p>
	 * If the path is not absolute, it will be resolved relative to the current
	 * working directory.
	 * </p>
	 * <p>
	 * Jar files can be specified instead of MANIFEST.MF file. If a jar file is
	 * specified, its MANIFEST.MF file will be read if it exists.
	 * </p>
	 *
	 * @param manifests the given extra manifest files' locations
	 */
	public void setExtraManifests(String manifests) {
		apiFileGenerator.manifests = manifests;
	}

	/**
	 * Set the extra source locations.
	 *
	 * <br>
	 * <br>
	 * This is a list of locations for source files that will be scanned. They are
	 * separated by the platform path separator. Each entry must exist. <br>
	 * <br>
	 * They should be specified using absolute paths.
	 *
	 * @param sourceLocations the given extra source locations
	 */
	public void setExtraSourceLocations(String sourceLocations) {
		apiFileGenerator.sourceLocations = sourceLocations;
	}

	@Override
	public void execute() {
		apiFileGenerator.generateAPIFile();
	}
}
