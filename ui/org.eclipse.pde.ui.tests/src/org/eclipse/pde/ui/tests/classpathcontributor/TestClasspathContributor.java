/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.classpathcontributor;

import java.util.*;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.ui.tests.classpathresolver.ClasspathResolverTest;

/**
 * Test classpath contributor that must be added as a extension for
 * {@link ClasspathContributorTest} to pass.
 *
 */
public class TestClasspathContributor implements IClasspathContributor {

	public static List<IClasspathEntry> entries;
	public static List<IClasspathEntry> entries2;
	static {
		IPath testPath = ResourcesPlugin.getWorkspace().getRoot().getFullPath().append(new Path("TestPath"));
		entries = new ArrayList<>();
		entries.add(JavaCore.newContainerEntry(testPath));
		entries.add(JavaCore.newLibraryEntry(testPath, null, null));
		entries.add(JavaCore.newProjectEntry(testPath));
		entries.add(JavaCore.newSourceEntry(testPath));
		IPath testPath2 = ResourcesPlugin.getWorkspace().getRoot().getFullPath().append(new Path("TestPath2"));
		entries2 = new ArrayList<>();
		entries2.add(JavaCore.newContainerEntry(testPath2));
		entries2.add(JavaCore.newLibraryEntry(testPath2, null, null));
		entries2.add(JavaCore.newProjectEntry(testPath2));
		entries2.add(JavaCore.newSourceEntry(testPath2));
	}

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		if (project.getSymbolicName().equals(ClasspathResolverTest.bundleName)){
			return entries;
		}
		return Collections.emptyList();
	}

	@Override
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency) {
		if (project.getSymbolicName().equals(ClasspathResolverTest.bundleName) && addedDependency.getSymbolicName().equals("org.eclipse.pde.core")){
			return entries2;
		}
		return Collections.emptyList();
	}

}
