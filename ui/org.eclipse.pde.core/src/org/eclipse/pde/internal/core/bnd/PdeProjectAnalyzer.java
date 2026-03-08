/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.natures.PluginProject;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

/**
 * An analyzer that is initialized by a {@link PdeProjectJar} and with the
 * resolved classpath of the project.
 */
public class PdeProjectAnalyzer extends Analyzer {

	public static final Set<String> DEFAULT_COPY_HEADER = Set.of(BUNDLE_CLASSPATH, BUNDLE_SYMBOLICNAME, BUNDLE_VERSION,
			BUNDLE_NAME, BUNDLE_LOCALIZATION, BUNDLE_VENDOR, BUNDLE_ACTIVATOR);

	public PdeProjectAnalyzer(IProject project, boolean includeTest) throws Exception {
		this(project, includeTest, DEFAULT_COPY_HEADER);
	}

	public PdeProjectAnalyzer(IProject project, boolean includeTest, Collection<String> copyHeader) throws Exception {
		super(includeTest ? new PdeTestProjectJar(project) : new PdeProjectJar(project));
		set(NOEXTRAHEADERS, "true"); //$NON-NLS-1$
		if (PluginProject.isJavaProject(project)) {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
			for (IClasspathEntry cp : classpath) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_LIBRARY && (includeTest || !cp.isTest())) {
					IPath path = cp.getPath();
					File file = path.toFile();
					if (file != null && file.getName().endsWith(".jar") && !file.getName().equals("jrt-fs.jar") //$NON-NLS-1$ //$NON-NLS-2$
							&& file.exists()) {
						addClasspath(file);
					}
				}
			}
		}
		Jar jar = getJar();
		Resource manifestResource = jar.getResource(JarFile.MANIFEST_NAME);
		if (manifestResource != null) {
			if (copyHeader.isEmpty()) {
				jar.remove(JarFile.MANIFEST_NAME);
			} else {
				Manifest mf;
				try (InputStream in = manifestResource.openInputStream()) {
					mf = new Manifest(in);
				}
				jar.remove(JarFile.MANIFEST_NAME);
				for (String header : copyHeader) {
					String value = mf.getMainAttributes().getValue(header);
					if (value != null) {
						set(header, value);
					}
				}
			}

		}
	}

}
