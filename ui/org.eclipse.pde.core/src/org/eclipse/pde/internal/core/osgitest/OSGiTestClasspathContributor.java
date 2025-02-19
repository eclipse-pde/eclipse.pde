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
package org.eclipse.pde.internal.core.osgitest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
 * If the users chose to add JUNIT container to a PDE project and OSGi test is
 * in the target platform this contributor automatically adds the OSGi test
 * extensions to the classpath.
 */
@Component(service = IClasspathContributor.class)
public class OSGiTestClasspathContributor implements IClasspathContributor, IStateDeltaListener {

	private static final IPath PATH = new Path("org.eclipse.jdt.junit.JUNIT_CONTAINER"); //$NON-NLS-1$

	private static final IPath JUNIT5_CONTAINER_PATH = PATH.append("5"); //$NON-NLS-1$
	private static final IPath JUNIT4_CONTAINER_PATH = PATH.append("4"); //$NON-NLS-1$

	private static final int CHANGE_FLAGS = BundleDelta.ADDED | BundleDelta.REMOVED | BundleDelta.UPDATED;

	private static final IClasspathAttribute TEST_ATTRIBUTE = JavaCore.newClasspathAttribute(IClasspathAttribute.TEST,
			"true"); //$NON-NLS-1$

	private static final Collection<String> OSGI_TEST_BUNDLES = List.of("org.osgi.test.common", //$NON-NLS-1$
			"org.osgi.test.assertj.framework", "org.osgi.test.assertj.log", "org.osgi.test.assertj.promise"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final Collection<String> OSGI_TEST_JUNIT5_BUNDLES = List.of("org.osgi.test.junit5", //$NON-NLS-1$
			"org.osgi.test.junit5.cm"); //$NON-NLS-1$

	private static final Collection<String> OSGI_TEST_JUNIT4_BUNDLES = List.of("org.osgi.test.junit4"); //$NON-NLS-1$

	private final ConcurrentMap<String, Collection<IClasspathEntry>> entryMap = new ConcurrentHashMap<>();

	@Activate
	void registerListener() {
		// TODO we need to listen to classpath changes as well, eg. container
		// added/removed/changed/...
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
	}

	@Deactivate
	void undregisterListener() {
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
	}

	/**
	 * @return s stream of all osgi test bundles
	 */
	public static Stream<String> bundles(boolean junit5) {
		return Stream.concat(OSGI_TEST_BUNDLES.stream(),
				junit5 ? OSGI_TEST_JUNIT5_BUNDLES.stream() : OSGI_TEST_JUNIT4_BUNDLES.stream());
	}

	@Override
	public List<IClasspathEntry> getInitialEntries(BundleDescription project) {
		IPluginModelBase projectModel = PluginRegistry.findModel((Resource) project);
		return junitBundles(projectModel)
				.map(bundleId -> entryMap.computeIfAbsent(bundleId,
						id -> ClasspathUtilCore
								.classpathEntriesForBundle(id, true, new IClasspathAttribute[] { TEST_ATTRIBUTE })
								.toList()))
				.flatMap(Collection::stream)
				.filter(Predicate.not(entry -> ClasspathUtilCore.isEntryForModel(entry, projectModel))).toList();
	}

	protected Stream<String> junitBundles(IPluginModelBase projectModel) {
		IResource resource = projectModel.getUnderlyingResource();
		if (resource != null) {
			IProject eclipseProject = resource.getProject();
			IJavaProject javaProject = JavaCore.create(eclipseProject);
			try {
				IClasspathEntry[] classpath = javaProject.getRawClasspath();
				for (IClasspathEntry cp : classpath) {
					if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						if (JUNIT5_CONTAINER_PATH.equals(cp.getPath())) {
							return bundles(true);
						}
						if (JUNIT4_CONTAINER_PATH.equals(cp.getPath())) {
							return bundles(false);
						}
					}
				}
			} catch (JavaModelException e) {
				// can't check, fall through and assume not enabled...
			}
		}
		return Stream.empty();
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
