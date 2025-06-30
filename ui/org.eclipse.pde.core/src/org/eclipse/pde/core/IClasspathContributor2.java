/*******************************************************************************
 * Copyright (c) 2025 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.pde.core;

import java.util.stream.Stream;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.resource.Resource;

/**
 * Extension interface for {@link org.eclipse.pde.core.IClasspathContributor}.
 * <p>
 * This interface allows adding classpath entries after the classpath has been
 * calculated.
 * </p>
 *
 * @since 3.21
 */
public interface IClasspathContributor2 extends IClasspathContributor {

	/**
	 * Get any additional classpath entries to add to a project when its
	 * classpath is first computed. The provided {@link BundleDescription}
	 * describes the plug-in project that the classpath is being computed for.
	 * The entries are added at the end of the calculation process. Additional
	 * PDE model information can be obtained using
	 * {@link PluginRegistry#findModel(Resource)}.
	 *
	 * @param project
	 *            the bundle descriptor for the plug-in project having its
	 *            classpath computed
	 * @return additional classpath entries to add to the project at the end of
	 *         the classpath calculation, possibly empty, must not be
	 *         <code>null</code>
	 * @since 3.21
	 */
	Stream<IClasspathEntry> getAdditionalEntries(BundleDescription project);
}
