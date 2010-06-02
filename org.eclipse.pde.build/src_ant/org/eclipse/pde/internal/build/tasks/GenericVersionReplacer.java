/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.File;
import org.apache.tools.ant.Task;

/**
 * Internal task.
 * Replace the version numbers of plugin.xml, fragment.xml and manifest.mf.
 * @since 3.0
 */
public class GenericVersionReplacer extends Task {
	private static final String FRAGMENT = "fragment.xml"; //$NON-NLS-1$
	private static final String PLUGIN = "plugin.xml"; //$NON-NLS-1$
	private static final String MANIFEST = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	private String rootPath;
	private String version;
	private String attributes;

	public void execute() {
		File root = new File(rootPath);
		if (root.exists() && root.isFile() && root.getName().equals(MANIFEST)) {
			callManifestModifier(rootPath);
			return;
		}

		File foundFile = new File(root, PLUGIN);
		if (foundFile.exists() && foundFile.isFile())
			callPluginVersionModifier(foundFile.getAbsolutePath(), PLUGIN);
		foundFile = new File(root, FRAGMENT);
		if (foundFile.exists() && foundFile.isFile())
			callPluginVersionModifier(foundFile.getAbsolutePath(), FRAGMENT);

		foundFile = new File(root, MANIFEST);
		if (foundFile.exists() && foundFile.isFile())
			callManifestModifier(foundFile.getAbsolutePath());
	}

	private void callPluginVersionModifier(String path, String input) {
		PluginVersionReplaceTask modifier = new PluginVersionReplaceTask();
		modifier.setProject(getProject());
		modifier.setPluginFilePath(path);
		modifier.setVersionNumber(version);
		modifier.setInput(input);
		modifier.execute();
	}

	private void callManifestModifier(String path) {
		ManifestModifier modifier = new ManifestModifier();
		modifier.setProject(getProject());
		modifier.setManifestLocation(path);
		modifier.setKeyValue("Bundle-Version|" + version); //$NON-NLS-1$
		if (attributes != null)
			modifier.setKeyValue(attributes);
		modifier.execute();
	}

	/**
	 * Set the path where the file to be replaced is contained.
	 * @param location path to the folder containing the file that needs to be replaced or the file path 
	 */
	public void setPath(String location) {
		this.rootPath = location;
	}

	/**
	 * Set the new version.
	 * @param version the version that will be set in the manifest file.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	public void setAttributes(String attributes) {
		this.attributes = attributes;
	}
}
