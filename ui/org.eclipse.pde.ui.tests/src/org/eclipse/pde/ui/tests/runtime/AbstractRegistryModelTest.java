/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.runtime;

import java.net.URISyntaxException;
import java.util.EventListener;
import junit.framework.TestCase;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.registry.model.*;
import org.eclipse.pde.internal.runtime.registry.model.Bundle;
import org.eclipse.pde.internal.runtime.registry.model.ServiceRegistration;
import org.eclipse.pde.ui.tests.PDETestsPlugin;
import org.osgi.framework.*;

public abstract class AbstractRegistryModelTest extends TestCase implements ModelChangeListener  {

	public static class MockFramework {
		private EventListener listener;

		public void createBundleEvent(int type, org.osgi.framework.Bundle bundle) {
			assertTrue(listener instanceof BundleListener);
			((BundleListener)listener).bundleChanged(new BundleEvent(type, bundle));
		}

		public void createRegistryAddedEvent(IExtension[] ext) {
			assertTrue(listener instanceof IRegistryEventListener);
			((IRegistryEventListener)listener).added(ext);
		}

		public void createRegistryRemovedEvent(IExtension[] ext) {
			assertTrue(listener instanceof IRegistryEventListener);
			((IRegistryEventListener)listener).removed(ext);
		}

		public void createRegistryAddedEvent(IExtensionPoint[] ext) {
			assertTrue(listener instanceof IRegistryEventListener);
			((IRegistryEventListener)listener).added(ext);
		}

		public void createRegistryRemovedEvent(IExtensionPoint[] ext) {
			assertTrue(listener instanceof IRegistryEventListener);
			((IRegistryEventListener)listener).removed(ext);
		}

		public void createServiceEvent(int type, ServiceReference<?> ref) {
			assertTrue(listener instanceof ServiceListener);
			((ServiceListener)listener).serviceChanged(new ServiceEvent(type, ref));
		}

		public void setListener(EventListener listener) {
			this.listener = listener;
		}
	}

	public static final String TEST_EXT_POINT = "org.eclipse.ui.views";
	public static final String TEST_EXT_POINT_BUNDLE = "org.eclipse.ui";

	protected MockFramework mockFramework = new MockFramework();

	protected org.osgi.framework.Bundle testBundle;
	protected ServiceReference<?> testServiceReference;

	protected IExtensionPoint testExtPoint;
	protected org.osgi.framework.Bundle testExtPointBundle;

	protected RegistryModel model;

	protected ModelChangeDelta[] deltas;

	@Override
	public void modelChanged(ModelChangeDelta[] deltas) {
		this.deltas = deltas;
	}

	abstract protected RegistryModel createModel() throws URISyntaxException;

	public AbstractRegistryModelTest() {
		testBundle = PDETestsPlugin.getBundleContext().getBundle();
		org.osgi.framework.ServiceRegistration<?> registration = PDETestsPlugin.getBundleContext()
				.registerService(getClass().getName(), this, null);
		testServiceReference = registration.getReference();

		testExtPoint = Platform.getExtensionRegistry().getExtensionPoint(TEST_EXT_POINT);
		testExtPointBundle = Platform.getBundle(TEST_EXT_POINT_BUNDLE);
	}

	@Override
	protected void setUp() throws Exception {
		model = createModel();
		model.connect(new NullProgressMonitor(), false);

		deltas = new ModelChangeDelta[0];
		model.addModelChangeListener(this);
	}

	@Override
	protected void tearDown() {
		model.removeModelChangeListener(this);
		model.disconnect();
	}

	/**
	 * Verifies that model provides correct list of installed bundles
	 */
	public void testInstalledBundles() {
		org.osgi.framework.Bundle[] origBundles = PDETestsPlugin.getBundleContext().getBundles();
		model.initialize(new NullProgressMonitor());
		Bundle[] bundles = model.getBundles();

		assertEquals(origBundles.length, bundles.length);
	}

	public void testBundleInstalled() {
		mockFramework.createBundleEvent(BundleEvent.INSTALLED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.ADDED, deltas[0].getFlag());
	}

	public void testBundleStartedEvent() {
		mockFramework.createBundleEvent(BundleEvent.STARTED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.STARTED, deltas[0].getFlag());
	}

	public void testBundleStoppedEvent() {
		mockFramework.createBundleEvent(BundleEvent.STOPPED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.STOPPED, deltas[0].getFlag());
	}

	public void testBundleUpdatedEvent() {
		mockFramework.createBundleEvent(BundleEvent.UPDATED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.UPDATED, deltas[0].getFlag());
	}

	public void testBundleUninstalledEvent() {
		mockFramework.createBundleEvent(BundleEvent.UNINSTALLED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.REMOVED, deltas[0].getFlag());
	}

	public void testBundleResolvedEvent() {
		mockFramework.createBundleEvent(BundleEvent.RESOLVED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.RESOLVED, deltas[0].getFlag());
	}

	public void testBundleUnresolvedEvent() {
		mockFramework.createBundleEvent(BundleEvent.UNRESOLVED, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.UNRESOLVED, deltas[0].getFlag());
	}

	public void testBundleStartingEvent() {
		mockFramework.createBundleEvent(BundleEvent.STARTING, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.STARTING, deltas[0].getFlag());
	}

	public void testBundleStoppingEvent() {
		mockFramework.createBundleEvent(BundleEvent.STOPPING, testBundle);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Bundle);
		assertEquals(testBundle.getSymbolicName(), ((Bundle)deltas[0].getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.STOPPING, deltas[0].getFlag());
	}

	public void testServiceRegisteredEvent() {
		mockFramework.createServiceEvent(ServiceEvent.REGISTERED, testServiceReference);

		assertEquals(2, deltas.length);
		ModelChangeDelta delta = deltas[0];
		assertTrue(delta.getModelObject() instanceof ServiceName);
		assertEquals(getClass().getName(), ((ServiceName)delta.getModelObject()).getClasses()[0]);
		assertEquals(ModelChangeDelta.ADDED, delta.getFlag());

		delta = deltas[1];
		assertTrue(delta.getModelObject() instanceof ServiceRegistration);
		assertEquals(getClass().getName(), ((ServiceRegistration)delta.getModelObject()).getName().getClasses()[0]);
		assertEquals(ModelChangeDelta.ADDED, delta.getFlag());
	}

	public void testServiceUnregisteringEvent() {
		mockFramework.createServiceEvent(ServiceEvent.UNREGISTERING, testServiceReference);

		assertEquals(2, deltas.length);
		ModelChangeDelta delta = deltas[0];
		assertTrue(delta.getModelObject() instanceof ServiceName);
		assertEquals(getClass().getName(), ((ServiceName)delta.getModelObject()).getClasses()[0]);
		assertEquals(ModelChangeDelta.REMOVED, delta.getFlag());

		delta = deltas[1];
		assertTrue(delta.getModelObject() instanceof ServiceRegistration);
		assertEquals(getClass().getName(), ((ServiceRegistration)delta.getModelObject()).getName().getClasses()[0]);
		assertEquals(ModelChangeDelta.REMOVED, delta.getFlag());
	}

	public void testServiceModifiedEvent() {
		mockFramework.createServiceEvent(ServiceEvent.MODIFIED, testServiceReference);

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof ServiceRegistration);
		assertEquals(getClass().getName(), ((ServiceRegistration)deltas[0].getModelObject()).getName().getClasses()[0]);
		assertEquals(ModelChangeDelta.UPDATED, deltas[0].getFlag());
	}

	public void testExtensionAddedEvent() {
		mockFramework.createRegistryAddedEvent(new IExtensionPoint[] {testExtPoint});

		IExtension ext = testExtPoint.getExtensions()[0];

		mockFramework.createRegistryAddedEvent(new IExtension[] {ext});

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Extension);
		assertEquals(ext.getLabel(), ((Extension)deltas[0].getModelObject()).getLabel());
		assertEquals(ext.getExtensionPointUniqueIdentifier(), ((Extension)deltas[0].getModelObject()).getExtensionPointUniqueIdentifier());
		assertEquals(ext.getNamespaceIdentifier(), ((Extension)deltas[0].getModelObject()).getNamespaceIdentifier());
		assertEquals(ModelChangeDelta.ADDED, deltas[0].getFlag());
	}

	public void testExtensionRemovedEvent() {
		mockFramework.createRegistryAddedEvent(new IExtensionPoint[] {testExtPoint});

		IExtension ext = testExtPoint.getExtensions()[0];

		mockFramework.createRegistryRemovedEvent(new IExtension[] {ext});

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof Extension);
		Extension modelObject = ((Extension)deltas[0].getModelObject());
		assertEquals(ext.getLabel(), modelObject.getLabel());
		assertEquals(ext.getExtensionPointUniqueIdentifier(), modelObject.getExtensionPointUniqueIdentifier());
		assertEquals(ext.getNamespaceIdentifier(), modelObject.getNamespaceIdentifier());
		assertEquals(ModelChangeDelta.REMOVED, deltas[0].getFlag());
	}

	public void testExtensionPointAddedEvent() {
		mockFramework.createRegistryAddedEvent(new IExtensionPoint[] {testExtPoint});

		assertEquals(1, deltas.length);
		assertTrue(deltas[0].getModelObject() instanceof ExtensionPoint);

		ExtensionPoint modelObject = ((ExtensionPoint)deltas[0].getModelObject());
		assertEquals(testExtPoint.getLabel(), modelObject.getLabel());
		assertEquals(testExtPoint.getNamespaceIdentifier(), modelObject.getNamespaceIdentifier());
		assertEquals(testExtPoint.getUniqueIdentifier(), modelObject.getUniqueIdentifier());
		assertEquals(ModelChangeDelta.ADDED, deltas[0].getFlag());
	}

	public void testExtensionPointRemovedEvent() {
		mockFramework.createRegistryRemovedEvent(new IExtensionPoint[] {testExtPoint});

		ExtensionPoint modelObject = ((ExtensionPoint)deltas[0].getModelObject());
		assertEquals(testExtPoint.getLabel(), modelObject.getLabel());
		assertEquals(testExtPoint.getNamespaceIdentifier(), modelObject.getNamespaceIdentifier());
		assertEquals(testExtPoint.getUniqueIdentifier(), modelObject.getUniqueIdentifier());
		assertEquals(ModelChangeDelta.REMOVED, deltas[0].getFlag());
	}
}
