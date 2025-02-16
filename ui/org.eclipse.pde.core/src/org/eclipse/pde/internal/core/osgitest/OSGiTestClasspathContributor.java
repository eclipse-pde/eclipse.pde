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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
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
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * If the users chose to add JUNIT container to a PDE project and OSGi test is
 * in the target platform this contributor automatically adds the OSGi test
 * extensions to the classpath.
 */
@Component(service = IClasspathContributor.class)
public class OSGiTestClasspathContributor
		implements IClasspathContributor, IStateDeltaListener, IResourceChangeListener {

	private static final int CHANGE_FLAGS = BundleDelta.ADDED | BundleDelta.REMOVED | BundleDelta.UPDATED;

	private static final IClasspathAttribute TEST_ATTRIBUTE = JavaCore.newClasspathAttribute(IClasspathAttribute.TEST,
			"true"); //$NON-NLS-1$

	private static final Collection<String> OSGI_TEST_BUNDLES = List.of("org.osgi.test.common", //$NON-NLS-1$
			"org.osgi.test.assertj.framework", "org.osgi.test.assertj.log", "org.osgi.test.assertj.promise"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final Collection<String> OSGI_TEST_JUNIT5_BUNDLES = List.of("org.osgi.test.junit5", //$NON-NLS-1$
			"org.osgi.test.junit5.cm"); //$NON-NLS-1$

	private static final Collection<String> OSGI_TEST_JUNIT4_BUNDLES = List.of("org.osgi.test.junit4"); //$NON-NLS-1$

	private static final JunitBundles JUNIT5 = new JunitBundles("JUnit 5", //$NON-NLS-1$
			() -> OSGiTestClasspathContributor.bundles(true));
	private static final JunitBundles JUNIT4 = new JunitBundles("JUnit 4", //$NON-NLS-1$
			() -> OSGiTestClasspathContributor.bundles(false));

	private static final JunitBundles NO_JUNIT = new JunitBundles("No JUnit", //$NON-NLS-1$
			() -> Stream.empty());

	private final ConcurrentMap<String, Collection<IClasspathEntry>> entryMap = new ConcurrentHashMap<>();
	private final Map<IProject, JunitProjectBundles> projectBundlesMap = new WeakHashMap<>();

	@Activate
	void registerListener() {
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	void setWorkspace(IWorkspace workspace) {
		workspace.addResourceChangeListener(this);
	}

	void unsetWorkspace(IWorkspace workspace) {
		workspace.removeResourceChangeListener(this);
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
		if (projectModel == null) {
			return List.of();
		}
		return getJunitBundles(projectModel.getUnderlyingResource()).bundles()
				.map(bundleId -> entryMap.computeIfAbsent(bundleId,
				id -> ClasspathUtilCore
						.classpathEntriesForBundle(id, true, new IClasspathAttribute[] { TEST_ATTRIBUTE }).toList()))
				.flatMap(Collection::stream)
				.filter(Predicate.not(entry -> ClasspathUtilCore.isEntryForModel(entry, projectModel))).toList();
	}

	private JunitBundles getJunitBundles(IResource resource) {
		if (resource == null) {
			return NO_JUNIT;
		}
		if (resource instanceof IProject project) {
			synchronized (projectBundlesMap) {
				return projectBundlesMap.computeIfAbsent(project, JunitProjectBundles::new).getBundles();
			}
		}
		return getJunitBundles(resource.getProject());
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

	private static final class JunitBundles {
		private Supplier<Stream<String>> bundleName;
		private String name;

		JunitBundles(String name, Supplier<Stream<String>> bundleName) {
			this.name = name;
			this.bundleName = bundleName;
		}

		@Override
		public String toString() {
			return name;
		}

		Stream<String> bundles() {
			return bundleName.get();
		}
	}

	private static final class JunitProjectBundles {

		private IProject eclipseProject;
		private JunitBundles bundles;

		JunitProjectBundles(IProject project) {
			this.eclipseProject = project;
		}

		synchronized JunitBundles getBundles() {
			if (bundles == null) {
				if (eclipseProject.isAccessible()) {
					IJavaProject javaProject = JavaCore.create(eclipseProject);
					try {
						IClasspathEntry[] classpath = javaProject.getRawClasspath();
						for (IClasspathEntry cp : classpath) {
							if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
								if (ICoreConstants.JUNIT5_CONTAINER_PATH.equals(cp.getPath())) {
									return bundles = JUNIT5;
								}
								if (ICoreConstants.JUNIT4_CONTAINER_PATH.equals(cp.getPath())) {
									return bundles = JUNIT4;
								}
							}
						}
					} catch (JavaModelException e) {
						// can't check, fall through and assume not enabled...
					}
				}
				return bundles = NO_JUNIT;
			}
			return bundles;
		}

		synchronized boolean update() {
			if (bundles == null) {
				return false;
			}
			JunitBundles old = bundles;
			bundles = null;
			return getBundles() != old;
		}

	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		Set<IProject> projects = new HashSet<>();
		try {
			IResourceDelta delta = event.getDelta();
			if (delta == null) {
				return;
			}
			delta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					if (resource instanceof IFile file) {
						if (file.getName().equals(".classpath")) { //$NON-NLS-1$
							projects.add(file.getProject());
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			// we tried our best
		}
		if (projects.isEmpty()) {
			// nothing more to do...
			return;
		}
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (IProject project : projects) {
			JunitProjectBundles projectBundles;
			synchronized (projectBundlesMap) {
				projectBundles = projectBundlesMap.get(project);
			}
			if (projectBundles != null && projectBundles.update()) {
				if (project.exists() && project.isOpen()) {
					IPluginModelBase model = manager.findModel(project);
					if (model != null) {
						try {
							JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH,
									new IJavaProject[] { JavaCore.create(project) },
									new IClasspathContainer[] { new RequiredPluginsClasspathContainer(model, project) },
									null);
						} catch (JavaModelException e) {
							// we can't update it...
						}
					}
				}
			}
		}

	}

}
