/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.classpathresolver;

import java.util.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.IBundleClasspathResolver;

public class TestBundleClasspathResolver implements IBundleClasspathResolver {

	public Map getAdditionalClasspathEntries(IJavaProject javaProject) {
		Map result = new LinkedHashMap();

		List paths = new ArrayList();

		paths.add(javaProject.getProject().getFolder("cpe").getLocation());

		result.put(new Path("library.jar"), paths);

		return result;
	}

	public Collection getAdditionalSourceEntries(IJavaProject javaProject) {
		List result = new ArrayList();

		IRuntimeClasspathEntry entry = JavaRuntime.newArchiveRuntimeClasspathEntry(javaProject.getProject().getFolder("cpe").getLocation());
		entry.setSourceAttachmentPath(entry.getPath());

		result.add(entry);

		return result;
	}

}
