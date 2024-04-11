/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IClasspathContributor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * This makes the <a href=
 * "https://docs.osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.bundle">OSGi
 * Bundle Annotations</a> and <a href=
 * "https://docs.osgi.org/specification/osgi.core/7.0.0/framework.api.html#org.osgi.annotation.versioning">OSGi
 * Versioning Annotations</a> available to plugin projects if they are part of
 * the target platform.
 */
@Component(service = IClasspathContributor.class)
public class OSGiAnnotationsClasspathContributor implements IClasspathContributor, IStateDeltaListener {

	private static final int CHANGE_FLAGS = BundleDelta.ADDED | BundleDelta.REMOVED | BundleDelta.UPDATED;

	private static final Collection<String> OSGI_ANNOTATIONS = List.of("org.osgi.annotation.versioning", //$NON-NLS-1$
			"org.osgi.annotation.bundle", "org.osgi.service.component.annotations", //$NON-NLS-1$ //$NON-NLS-2$
			"org.osgi.service.metatype.annotations"); //$NON-NLS-1$

	private ConcurrentMap<String, Collection<IClasspathEntry>> entryMap = new ConcurrentHashMap<>();

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		IPluginModelBase projectModel = PluginRegistry.findModel((Resource) project);
		return annotations()
				.map(bundleId -> entryMap.computeIfAbsent(bundleId,
						id -> ClasspathUtilCore.classpathEntriesForBundle(id).toList()))
				.flatMap(Collection::stream)
				.filter(Predicate.not(entry -> ClasspathUtilCore.isEntryForModel(entry, projectModel))).toList();
	}

	@Activate
	void registerListener() {
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
	}

	@Deactivate
	void undregisterListener() {
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
	}

	/**
	 * @return s stream of all current available annotations in the current
	 *         plugin registry
	 */
	public static Stream<String> annotations() {
		return OSGI_ANNOTATIONS.stream();
	}

	@Override
	public List<IClasspathEntry> getEntriesForDependency(BundleDescription project, BundleDescription addedDependency) {
		return Collections.emptyList();
	}

	@Override
	public void stateResolved(StateDelta delta) {
		if (delta == null) {
			stateChanged(null);
		} else {
			// just refresh the items in the map if they have any changes...
			for (BundleDelta bundleDelta : delta.getChanges(CHANGE_FLAGS, false)) {
				entryMap.remove(bundleDelta.getBundle().getSymbolicName());
			}
		}

	}

	@Override
	public void stateChanged(State newState) {
		// we need to refresh everything
		entryMap.clear();
	}

}
