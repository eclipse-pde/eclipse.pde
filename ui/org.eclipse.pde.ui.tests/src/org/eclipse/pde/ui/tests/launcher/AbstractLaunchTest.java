/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *     Andras Peteri <apeteri@b2international.com> - extracted common superclass
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.*;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public abstract class AbstractLaunchTest {

	private IProject project;

	@BeforeClass
	public static void setupTargetPlatform() throws Exception {
		TargetPlatformUtil.setRunningPlatformAsTarget();
	}

	@Before
	public void setupLaunchConfigurations() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject(getClass().getSimpleName());
		if (project.exists()) {
			project.delete(true, null);
		}

		project.create(null);
		project.open(null);
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		ArrayList<URL> resources = Collections.list(bundle.findEntries("tests/launch", "*", false));
		for (URL url : resources) {
			Path path = Paths.get(FileLocator.toFileURL(url).toURI());
			IFile file = project.getFile(path.getFileName().toString());
			try (InputStream in = url.openStream()) {
				file.create(in, true, null);
			}
		}
	}

	protected ILaunchConfiguration getLaunchConfiguration(String name) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		return launchManager.getLaunchConfiguration(project.getFile(name));
	}
}
