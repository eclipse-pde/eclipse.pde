/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
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
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 541067
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetEvents;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.PDETestCase;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventHandler;

/**
 * Common utility methods for target definition tests
 */
public abstract class AbstractTargetTest extends PDETestCase {

	/**
	 * Returns the target platform service or <code>null</code> if none
	 *
	 * @return target platform service
	 */
	protected static ITargetPlatformService getTargetService() {
		return org.eclipse.pde.internal.core.target.TargetPlatformService.getDefault();
	}

	/**
	 * Extracts bundles a through e and returns a path to the root directory containing
	 * the plug-ins.
	 *
	 * @return path to the plug-ins directory
	 */
	protected Path extractAbcdePlugins() throws Exception {
		Path stateLocation = getThisBundlesStateLocation();
		Path location = stateLocation.resolve("abcde-plugins");
		if (Files.exists(location)) {
			// recursively delete
			delete(location.toFile());
		}
		return doUnZip(stateLocation, "/tests/targets/abcde-plugins.zip");
	}

	/**
	 * Extracts the modified jdt features archive, if not already done, and returns a path to the
	 * root directory containing the features and plug-ins
	 *
	 * @return path to the root directory
	 */
	protected Path extractModifiedFeatures() throws Exception {
		Path stateLocation = getThisBundlesStateLocation();
		Path location = stateLocation.resolve("modified-jdt-features");
		if (Files.exists(location)) {
			return location;
		}
		doUnZip(stateLocation, "/tests/targets/modified-jdt-features.zip");
		// If we are not on the mac, delete the mac launching bundle (in a standard non Mac build, the plug-in wouldn't exist)
		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			Path macBundle = location.resolve("plugins")
					.resolve("org.eclipse.jdt.launching.macosx_3.2.0.v20090527.jar");
			Files.deleteIfExists(macBundle);
		}
		return location;
	}


	/**
	 * Extracts the multiple versions plug-ins archive, if not already done, and returns a path to the
	 * root directory containing the plug-ins.
	 *
	 * @return path to the directory containing the bundles
	 */
	protected Path extractMultiVersionPlugins() throws Exception {
		Path stateLocation = getThisBundlesStateLocation();
		Path location = stateLocation.resolve("multi-versions");
		if (Files.exists(location)) {
			return location;
		}
		doUnZip(stateLocation, "/tests/targets/multi-versions.zip");
		return location;
	}

	/**
	 * Used to reset the target platform to original settings after a test that
	 * changes the target platform.
	 */
	protected void resetTargetPlatform() throws CoreException {
		ITargetDefinition definition = getDefaultTargetPlatorm();
		setTargetPlatform(definition);
	}

	/**
	 * Returns a new target definition from the target service.  This method is
	 * overridden by {@link WorkspaceTargetDefinitionTests} to use a workspace
	 * target definition
	 *
	 * @return a new target definition
	 */
	protected ITargetDefinition getNewTarget() {
		return getTargetService().newTarget();
	}

	/**
	 * Returns a default target platform that takes target weaving into account
	 * if in a second instance of Eclipse. This allows the target platform to be
	 * reset after changing it in a test.
	 *
	 * @return default settings for target platform
	 */
	protected ITargetDefinition getDefaultTargetPlatorm() {
		ITargetDefinition definition = getNewTarget();
		ITargetLocation container = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		definition.setTargetLocations(new ITargetLocation[]{container});
		return definition;
	}

	/**
	 * Synchronously sets the target platform based on the given definition. This method should
	 * be called inside of a try/finally block that will always call {@link #resetTargetPlatform()}
	 *
	 * @param target target definition or <code>null</code>
	 */
	protected void setTargetPlatform(ITargetDefinition target) throws CoreException {
		final AtomicReference<Object> payload = new AtomicReference<>();
		BundleContext bundleContext = PDECore.getDefault().getBundleContext();
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		EventHandler handler = e -> payload.compareAndSet(null, e.getProperty(IEventBroker.DATA));
		eventBroker.subscribe(TargetEvents.TOPIC_WORKSPACE_TARGET_CHANGED, handler);

		// Create the job to load the target, but then join with the job's thread
		try {
			TargetPlatformUtil.loadAndSetTarget(target);
		} catch (InterruptedException e) {
			assertFalse("Target platform reset interrupted", true);
		}
		TestUtils.waitForJobs(name.getMethodName(), 100, 30000);
		Object firstDefinition = payload.getAndSet(null);

		ITargetPlatformService service = getTargetService();
		// this call will trigger more events if the target was null
		ITargetDefinition definition = (target != null) ? target : service.getWorkspaceTargetDefinition();
		TestUtils.waitForJobs(name.getMethodName(), 100, 30000);
		eventBroker.unsubscribe(handler);
		Object secondDefinition = payload.get();
		ITargetHandle handle = (target != null) ? target.getHandle() : null;
		assertEquals("Wrong target platform handle preference setting", handle, service.getWorkspaceTargetHandle());
		if (target == null) {
			assertEquals("Wrong workspaceTargetChanged event payload", definition, secondDefinition);
		} else {
			assertEquals("Wrong workspaceTargetChanged event payload", definition, firstDefinition);
		}

	}

	/**
	 * Collects all bundle symbolic names into a set.
	 *
	 * @param infos bundles
	 * @return bundle symbolic names
	 */
	protected Set<String> collectAllSymbolicNames(List<BundleInfo> infos) {
		Set<String> set = new HashSet<>(infos.size());
		for (BundleInfo info : infos) {
			set.add(info.getSymbolicName());
		}
		return set;
	}

	/**
	 * Retrieves all bundles (source and code) in the given target definition
	 * returning them as a list of BundleInfos.
	 *
	 * @param target target definition
	 * @return all BundleInfos
	 */
	protected List<BundleInfo> getAllBundleInfos(ITargetDefinition target) throws Exception {
		if (!target.isResolved()) {
			target.resolve(null);
		}
		TargetBundle[] bundles = target.getBundles();
		List<BundleInfo> list = new ArrayList<>(bundles.length);
		for (TargetBundle bundle : bundles) {
			list.add(bundle.getBundleInfo());
		}
		return list;
	}

	/**
	 * Returns a list of bundles included in the given container.
	 *
	 * @param container bundle container
	 * @return included bundles
	 */
	protected List<BundleInfo> getBundleInfos(ITargetLocation container) throws Exception {
		TargetBundle[] bundles = container.getBundles();
		List<BundleInfo> list = new ArrayList<>(bundles.length);
		for (TargetBundle bundle : bundles) {
			list.add(bundle.getBundleInfo());
		}
		return list;
	}
}
