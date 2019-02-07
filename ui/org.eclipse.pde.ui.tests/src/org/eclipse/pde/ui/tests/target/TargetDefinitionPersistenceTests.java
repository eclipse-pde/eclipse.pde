/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.target;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.TargetDefinitionPersistenceHelper;

/**
 * Tests the persistence of target definitions.  Tests memento creation, reading of old target files, and writing of the model.
 *
 * @since 3.5
 */
public class TargetDefinitionPersistenceTests extends MinimalTargetDefinitionPersistenceTests {

	/**
	 * Tests that a complex metadata based target definition can be serialized to xml,
	 * then deserialized without any loss of data.
	 *
	 * @throws Exception
	 */

	// @IgnoreWhen
	public void testPersistComplexMetadataDefinition() throws Exception {
		// org.junit.Assume.assumeTrue(false);
		ITargetDefinition definitionA = getTargetService().newTarget();
		initComplexDefiniton(definitionA);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);

		assertTargetDefinitionsEqual(definitionA, definitionB);
	}

	/**
	 * Tests that a complex workspace file based target definition can be
	 * serialized to xml, then deserialized without any loss of data.
	 *
	 * @throws Exception
	 */
	public void testPersistComplexWorkspaceDefinition() throws Exception {
		// org.junit.Assume.assumeTrue(false);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("TargetDefinitionPersistenceTests");
		try {
			if (!project.exists()) {
				project.create(null);
			}
			assertTrue("Could not create test project", project.exists());
			project.open(null);
			assertTrue("Could not open test project", project.isOpen());

			IFile target = project.getFile(Long.toString(System.currentTimeMillis()) + "A.target");
			ITargetDefinition definitionA = getTargetService().getTarget(target).getTargetDefinition();
			initComplexDefiniton(definitionA);
			getTargetService().saveTargetDefinition(definitionA);
			ITargetDefinition definitionB = getTargetService().getTarget(target).getTargetDefinition();

			assertTargetDefinitionsEqual(definitionA, definitionB);
		} finally {
			if (project.exists()) {
				project.delete(true, null);
			}
			assertFalse("Could not delete test project", project.exists());
		}
	}

	protected void initComplexDefiniton(ITargetDefinition definition) throws URISyntaxException {
		definition.setName("name");
		definition.setOS("os");
		definition.setWS("ws");
		definition.setArch("arch");
		definition.setNL("nl");
		definition.setProgramArguments("program\nargs");
		definition.setVMArguments("vm\nargs");
		definition.setJREContainer(JavaRuntime.newDefaultJREContainerPath());

		NameVersionDescriptor[] implicit = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null) };
		definition.setImplicitDependencies(implicit);

		// Directory container
		ITargetLocation dirContainer = getTargetService()
				.newDirectoryLocation(TargetPlatform.getDefaultLocation() + "/plugins");
		// Profile container with specific config area
		ITargetLocation profileContainer = getTargetService().newProfileLocation(TargetPlatform.getDefaultLocation(),
				new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		// Feature container with specific version
		IPath location = getJdtFeatureLocation();
		String segment = location.lastSegment();
		int index = segment.indexOf('_');
		assertTrue("Missing version id", index > 0);
		String version = segment.substring(index + 1);
		ITargetLocation featureContainer = getTargetService().newFeatureLocation("${eclipse_home}", "org.eclipse.jdt",
				version);
		// Profile container restricted to just two bundles
		ITargetLocation restrictedProfileContainer = getTargetService()
				.newProfileLocation(TargetPlatform.getDefaultLocation(), null);

		// Site bundle containers with different settings
		IUBundleContainer siteContainer = (IUBundleContainer) getTargetService().newIULocation(
				new IInstallableUnit[] {}, new URI[] { new URI("TESTURI"), new URI("TESTURI2") },
				IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS);
		IUBundleContainer siteContainer2 = (IUBundleContainer) getTargetService().newIULocation(
				new String[] { "unit1", "unit2" }, new String[] { "1.0", "2.0" },
				new URI[] { new URI("TESTURI"), new URI("TESTURI2") }, IUBundleContainer.INCLUDE_REQUIRED);

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.jdt.launching", null),
				new NameVersionDescriptor("org.eclipse.jdt.debug", null) };
		definition.setIncluded(restrictions);
		definition.setTargetLocations(new ITargetLocation[] { dirContainer, profileContainer, featureContainer,
				restrictedProfileContainer, siteContainer, siteContainer2 });
	}


}