/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Registry change listeners can be added to and removed from the PDE extension
 * registry while it has no underlying registry, because it was not created yet
 * or was discarded by a target reload. A listener added meanwhile receives the
 * events of the registry created later, a listener removed meanwhile does not.
 */
public class PDEExtensionRegistryListenerTest {

	private static final long EVENT_TIMEOUT_SECONDS = 30;

	@Rule
	public final TestRule clearWorkspace = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	private final PDEExtensionRegistry registry = PDECore.getDefault().getExtensionsRegistry();
	private final List<IRegistryChangeListener> addedListeners = new ArrayList<>();

	@After
	public void removeListeners() {
		addedListeners.forEach(registry::removeListener);
	}

	@Test
	public void testListenerAddedAfterTargetReloadReceivesEvents() throws Exception {
		String namespace = "registry.listener.added";
		registry.targetReloaded();
		RecordingListener listener = addListener(namespace);
		createRegistry();

		contributeExtension(namespace);

		assertTrue("listener received no registry change event", listener.awaitDeltas(EVENT_TIMEOUT_SECONDS));
	}

	@Test
	public void testListenerRemovedAfterTargetReloadReceivesNoEvents() throws Exception {
		String namespace = "registry.listener.removed";
		createRegistry();
		RecordingListener listener = addListener(namespace);
		registry.targetReloaded();
		registry.removeListener(listener);
		// attached after the removed listener would be, so it is notified later
		RecordingListener sentinel = addListener(namespace);
		createRegistry();

		contributeExtension(namespace);

		assertTrue("sentinel received no registry change event", sentinel.awaitDeltas(EVENT_TIMEOUT_SECONDS));
		assertFalse("removed listener received a registry change event", listener.hasDeltas());
	}

	@Test
	public void testAddListenerDoesNotCreateRegistry() {
		PDEExtensionRegistry uncreatedRegistry = new PDEExtensionRegistry(new IPluginModelBase[0]) {
			@Override
			protected IExtensionRegistry getRegistry() {
				throw new AssertionError("registry must not be created");
			}
		};
		IRegistryChangeListener listener = event -> {
		};
		try {
			uncreatedRegistry.addListener(listener);
			uncreatedRegistry.removeListener(listener);
		} finally {
			uncreatedRegistry.dispose();
		}
	}

	private RecordingListener addListener(String namespace) {
		RecordingListener listener = new RecordingListener(namespace);
		addedListeners.add(listener);
		registry.addListener(listener);
		return listener;
	}

	private void createRegistry() {
		registry.findExtensions("org.eclipse.pde.core.source", false);
	}

	/**
	 * Creates a plug-in project declaring an extension point and an extension to
	 * it, which the PDE extension registry adds as a contribution in the
	 * namespace of the given symbolic name.
	 */
	private static void contributeExtension(String symbolicName) throws CoreException {
		IProject project = ProjectUtils.createPluginProject(symbolicName, symbolicName, "1.0.0",
				(description, service) -> description.setSingleton(true));
		ProjectUtils.createFile(project, IPath.fromOSString("plugin.xml"), """
				<?xml version="1.0" encoding="UTF-8"?>
				<?eclipse version="3.4"?>
				<plugin>
				   <extension-point id="point" name="point"/>
				   <extension point="%s.point">
				      <element/>
				   </extension>
				</plugin>
				""".formatted(symbolicName));
	}

	private static final class RecordingListener implements IRegistryChangeListener {

		private final String namespace;
		private boolean receivedDeltas;

		RecordingListener(String namespace) {
			this.namespace = namespace;
		}

		@Override
		public synchronized void registryChanged(IRegistryChangeEvent event) {
			// the event and its deltas are valid only during this call
			if (event.getExtensionDeltas(namespace).length > 0) {
				receivedDeltas = true;
				notifyAll();
			}
		}

		synchronized boolean hasDeltas() {
			return receivedDeltas;
		}

		synchronized boolean awaitDeltas(long timeoutSeconds) throws InterruptedException {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
			while (!receivedDeltas) {
				long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
				if (remainingMillis <= 0) {
					return false;
				}
				wait(remainingMillis);
			}
			return true;
		}
	}
}
