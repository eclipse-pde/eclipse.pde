/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      IBM Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.core;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

/**
 * Resolves dynamically generated bundle classpath entries in the context of a java project.
 * 
 * <p>
 * Generally, dynamically generated bundle classpath entries are not present under project
 * source tree but included in the bundle as part build process. During development time such bundle classpath entries
 * can be resolved to external jar files or workspace resources. Resolution of the
 * same entry may change over time, similarly to how Plug-in Dependencies classpath container can switch between 
 * external bundles and workspace projects.
 * </p>
 * 
 * <p>
 * A resolver is declared as an extension (<code>org.eclipse.pde.core.bundleClasspathResolvers</code>). This 
 * extension has the following attributes: 
 * <ul>
 * <li><code>nature</code> specified nature of the projects this resolver is registered for.</li>
 * <li><code>class</code> specifies the fully qualified name of the Java class that implements 
 *     <code>IBundleClasspathResolver</code>.</li>
 * </ul>
 * </p>
 * <p> 
 * The resolver is consulted when dynamically generated bundle is added to OSGi runtime launch and when looking up 
 * sources from the bundle.
 * </p>
 * 
 * @since 3.8
 */
public interface IBundleClasspathResolver {

	/**
	 * Returns a possibly empty map describing additional bundle classpath entries for a project in the workspace.
	 * 
	 * <p>The map key is a {@link IPath} describing the project relative path to a source directory or library.  The value
	 * is the {@link Collection} of {@link IPath} locations (relative to the project or absolute) that should be added
	 * to the bundle classpath.</p>
	 * 
	 * @param javaProject the java project to collect classpath entries for
	 * @return additional entries to add to the bundle classpath. Map of IPath to Collection, possibly empty
	 */
	public Map/*<IPath, Collection<IPath>>*/getAdditionalClasspathEntries(IJavaProject javaProject);

	/**
	 * Returns a possibly empty collection listing additional classpath entries for the source lookup path of a project in the workspace.
	 * 
	 * <p>The {@link Collection} will contain {@link IRuntimeClasspathEntry} describing locations where source can be obtained from.</p>
	 * 
	 * @param javaProject the java project to collect source entries for
	 * @return additional entries for the source lookup path. Collection of IRuntimeClasspathEntry, possibly empty
	 */
	public Collection/*<IRuntimeClasspathEntry>*/getAdditionalSourceEntries(IJavaProject javaProject);
}
