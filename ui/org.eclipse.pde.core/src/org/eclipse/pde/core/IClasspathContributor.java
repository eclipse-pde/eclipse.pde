/*******************************************************************************
 * Copyright (c) 2013 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     BestSolution.at - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.core;

import java.util.List;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.PluginRegistry;

/**
 * Implementors of this interface can contribute additional {@link IClasspathEntry}
 * to a plug-in project as the classpath is computed. The contributor is consulted
 * when the initial classpath for a plug-in project is calculated as well as whenever
 * a new bundle dependency is created.
 * <p>
 * A classpath contributor is declared as an extension (<code>org.eclipse.pde.core.pluginClasspathContributors</code>).
 * </p>
 * <p>
 * The added classpath entries are only stored as long as the project classpath is and will
 * not be considered during plug-in or feature export.
 * </p>
 *
 * @since 3.9
 */
public interface IClasspathContributor {

	/**
	 * Get any additional classpath entries to add to a project when its classpath is
	 * first computed.  The provided {@link BundleDescription} describes the plug-in
	 * project that the classpath is being computed for.  Additional PDE model information
	 * can be obtained using {@link PluginRegistry#findModel(BundleDescription)}.
	 *
	 * @param project the bundle descriptor for the plug-in project having its classpath computed
	 * @return additional classpath entries to add to the project, possibly empty, must not be <code>null</code>
	 */
	public List<IClasspathEntry> getInitialEntries(BundleDescription project);

	/**
	 * Get any additional classpath entries to add to a project when a new bundle
	 * is being added to the project classpath as a dependency.  The {@link BundleDescription}
	 * is provided for both the plug-in that the classpath is being calculated for and
	 * the dependency being added.  The dependency may be a project in the workspace or an
	 * external bundle from the target platform.
	 *
	 * @param project the bundle descriptor for the plug-in project having its classpath computed
	 * @param addedDependency the bundle descriptor for the bundle being added to the classpath as a dependency
	 * @return additional classpath entries to add to the project, possibly empty, must not be <code>null</code>
	 */
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency);
}
