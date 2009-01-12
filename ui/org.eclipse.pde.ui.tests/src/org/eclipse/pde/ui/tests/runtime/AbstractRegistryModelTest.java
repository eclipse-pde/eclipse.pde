/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.runtime;

import java.util.*;
import junit.framework.TestCase;
import org.eclipse.pde.internal.runtime.registry.model.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public abstract class AbstractRegistryModelTest extends TestCase {

	public static class TestModelChangeListener implements ModelChangeListener {
		private List fDeltas = new ArrayList();
		private int notificationsCount = 0;
		private Thread testsThread;
		
		public TestModelChangeListener() {
			// get the thread in which tests are run
			testsThread = Thread.currentThread();
		}
		
		public void modelChanged(ModelChangeDelta[] deltas) {
			notificationsCount++;
			fDeltas.addAll(Arrays.asList(deltas));
			
			// notify tests thread if it's waiting in waitForNotifications sleep
			testsThread.interrupt();
		}
		
		public ModelChangeDelta[] getDeltas() {
			return (ModelChangeDelta[]) fDeltas.toArray(new ModelChangeDelta[fDeltas.size()]);
		}
		
		public int waitForNotifications(int count) {
			
			while (notificationsCount <= count) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				
				count--;
			}
			return notificationsCount;
		}
	}

	private static final String TESTBUNDLE_PATH = "/testplugins/org.eclipse.pde.runtime.tests.testbundle_1.0.0.jar";

	private static final String TESTBUNDLE = "org.eclipse.pde.runtime.tests.testbundle";
	
	protected RegistryModel f;
	
	abstract protected RegistryModel createModel();
	
	protected void setUp() {
		f = createModel();
		f.connect();
	}
	
	protected void tearDown() {
		f.disconnect();
	}
	
	/**
	 * Verifies that model provides correct list of installed bundles 
	 */
	public void testInstalledBundles() {
		int bundlesCount = 0;
		org.osgi.framework.Bundle[] origBundles = MacroPlugin.getBundleContext().getBundles();
		
		// ignore all bundle fragments
		for (int i = 0; i < origBundles.length; i++) {
			if (origBundles[i].getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				bundlesCount++;
			}
		}
		
		Bundle[] bundles = f.getBundles();
		
		assertEquals(bundlesCount, bundles.length);
	}
	
	public void testBundleChangedNotification() throws BundleException {
		TestModelChangeListener listener = new TestModelChangeListener();
		
		f.addModelChangeListener(listener);
		org.osgi.framework.Bundle bundle = MacroPlugin.getBundleContext().installBundle(TestUtils.findPath(TESTBUNDLE_PATH));
		bundle.start(); // resolved, started
		bundle.stop(); // stopped, unresolved
		bundle.update();
		bundle.uninstall();
		
		listener.waitForNotifications(7);

		f.removeModelChangeListener(listener);
		
		ModelChangeDelta[] deltas = listener.getDeltas();
		
		// bundle install
		ModelChangeDelta delta = deltas[0];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.ADDED, delta.getFlag());
		
		// bundle starting
		delta = deltas[1];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.RESOLVED, delta.getFlag());
		
		// bundle started
		delta = deltas[2];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.STARTED, delta.getFlag());
		
		// bundle stopping
		delta = deltas[3];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.STOPPED, delta.getFlag());
		
		// bundle stopped
		delta = deltas[4];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.UNRESOLVED, delta.getFlag());
		
		// bundle update
		delta = deltas[5];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.UPDATED, delta.getFlag());
		
		// bundle uninstall
		delta = deltas[6];
		assertTrue(delta.getModelObject() instanceof Bundle);
		assertEquals(TESTBUNDLE, ((Bundle)delta.getModelObject()).getSymbolicName());
		assertEquals(ModelChangeDelta.REMOVED, delta.getFlag());
		
		assertEquals(7, deltas.length);
	}
	
	public void testServiceChangedNotification() throws BundleException {
		TestModelChangeListener listener = new TestModelChangeListener();
		
		f.addModelChangeListener(listener);
		org.osgi.framework.ServiceRegistration registration = MacroPlugin.getBundleContext().registerService(getClass().getName(), this, null);
		registration.unregister();
		
		listener.waitForNotifications(2);
		
		f.removeModelChangeListener(listener);
		
		ModelChangeDelta[] deltas = listener.getDeltas();
		
		// service register
		ModelChangeDelta delta = deltas[0];
		assertTrue(delta.getModelObject() instanceof ServiceName);
		assertEquals(getClass().getName(), ((ServiceName)delta.getModelObject()).getClasses()[0]);
		assertEquals(ModelChangeDelta.ADDED, delta.getFlag());
		
		// service unregister
//		delta = deltas[1];
//		assertTrue(delta.getModelObject() instanceof ServiceRegistration);
//		assertEquals(getClass().getName(), ((ServiceRegistration)delta.getModelObject()).getName().getClasses()[0]);
//		assertEquals(ModelChangeDelta.REMOVED, delta.getFlag());
//		
//		assertEquals(2, deltas.length);
	}

}
