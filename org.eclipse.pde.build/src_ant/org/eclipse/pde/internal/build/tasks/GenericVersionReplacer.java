/**********************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.File;
import org.apache.tools.ant.Task;

public class GenericVersionReplacer extends Task {
	private String path;
	private String version;

	public void execute() {
		File root = new File(path);
		if (root.exists() && root.isFile() && root.getName().equals("META-INF/MANIFEST.MF")) {
			callManifestModifier(path);
			return;
		}

		File foundFile = new File(root, "plugin.xml");
		if (foundFile.exists() && foundFile.isFile())
			callPluginVersionModifier(foundFile.getAbsolutePath());
		foundFile = new File(root, "fragment.xml");
		if (foundFile.exists() && foundFile.isFile())
			callPluginVersionModifier(foundFile.getAbsolutePath());

		foundFile = new File(root, "META-INF/MANIFEST.MF");
		if (foundFile.exists() && foundFile.isFile())
			callManifestModifier(foundFile.getAbsolutePath());
	}

	private void callPluginVersionModifier(String path) {
		PluginVersionReplaceTask modifier = new PluginVersionReplaceTask();
		modifier.setProject(getProject());
		modifier.setPluginFilePath(path);
		modifier.setVersionNumber(version);
		modifier.execute();
	}

	private void callManifestModifier(String loc) {
		ManifestModifier modifier = new ManifestModifier();
		modifier.setProject(getProject());
		modifier.setManifestLocation(loc);
		modifier.setKeyValue("Bundle-Version|" + version);
		modifier.execute();
	}

	public void setPath(String location) {
		this.path = location;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}