/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

public class TargetNewAPIResolutionTests extends AbstractTargetTest {
	
	public static Test suite() {
		return new TestSuite(TargetNewAPIResolutionTests.class);
	}
	
	public void testLocationUnitCreation() throws Exception {
		IProvisioningAgent agent = (IProvisioningAgent)PDECore.getDefault().acquireService(IProvisioningAgent.SERVICE_NAME);
		assertNotNull(agent);
		
		// Directory container
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		IRepository[] repos = directoryContainer.generateRepositories(agent, null);
		assertNotNull(repos);
		assertEquals(1, repos.length);
		InstallableUnitDescription[] units = directoryContainer.getRootIUs(agent, null);
		assertNotNull(units);
		assertTrue(units.length > 1);

		// Installation container
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		repos = profileContainer.generateRepositories(agent, null);
		assertNotNull(repos);
		assertEquals(1, repos.length);
		units = profileContainer.getRootIUs(agent, null);
		assertNotNull(units);
		assertTrue(units.length > 1);
		
		// Repository container
		IBundleContainer iuContainer = getTargetService().newIUContainer(new String[]{"Test", "Test2"}, new String[]{"1", "1.1.1"});
		repos = iuContainer.generateRepositories(agent, null);
		assertNotNull(repos);
		assertEquals(0, repos.length);
		units = iuContainer.getRootIUs(agent, null);
		assertNotNull(units);
		assertEquals(2,units.length);
		
		// Feature container
		IBundleContainer featureContainer = getTargetService().newFeatureContainer(TargetPlatform.getDefaultLocation(), "org.eclipse.jdt", null);
		repos = featureContainer.generateRepositories(agent, null);
		assertNotNull(repos);
		assertEquals(1, repos.length);
		units = featureContainer.getRootIUs(agent, null);
		assertNotNull(units);
		assertTrue(units.length > 1);
	}
	
	public void testTargetBasicResolution() throws Exception {
		ITargetDefinition definition = getNewTarget();
		IBundleContainer directoryContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		IBundleContainer iuContainer = getTargetService().newIUContainer(new String[]{"Test", "Test2"}, new String[]{"1", "1.1.1"});
		definition.setBundleContainers(new IBundleContainer[]{directoryContainer, profileContainer, iuContainer});
		
		assertNull(definition.getResolveStatus());
		assertNull(definition.getAvailableUnits());
		assertNull(definition.getIncludedUnits(null));
		assertNull(definition.getMissingUnits(null));
		
		IStatus status = definition.resolve(null);
		assertTrue("Resolve status: " + status,status.isOK());
		assertTrue(definition.getResolveStatus().isOK());
		assertTrue(definition.getAvailableUnits().length > 0);
		assertEquals(definition.getIncludedUnits(null).length,definition.getAvailableUnits().length);
		assertEquals(0,definition.getMissingUnits(null).length);
		
		definition.addIncluded(new BundleInfo[]{new BundleInfo("org.eclipse.platform",null,null,BundleInfo.NO_LEVEL,false), new BundleInfo("does.not.exist",null,null,BundleInfo.NO_LEVEL,false)});
		assertTrue(definition.getResolveStatus().isOK());
		assertTrue(definition.getAvailableUnits().length > 0);
		// TODO Incomplete
//		assertEquals(1,definition.getIncludedUnits(null).length);
//		assertEquals(1,definition.getMissingUnits(null).length);
		
		status = definition.provision(null);
		assertTrue("Provision status: " + status, status.isOK());
		
	}
	
}
	
	