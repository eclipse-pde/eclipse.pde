/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.genericeditor.extension.tests.resources.TestTargetLocation;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.TargetDefinitionPersistenceHelper;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class Bug531602FormattingTests extends AbstractTargetEditorTest {

	@Test
	public void testSettingNullPersists() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		targetDefinition.setName("test");
		ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, expectedOutput);

		ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
		targetDefinition.setProgramArguments(null);
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, actualOutput);
		assertEquals(expectedOutput.toString(StandardCharsets.UTF_8.toString()),
				actualOutput.toString(StandardCharsets.UTF_8.toString()));
	}

	@Test
	public void testIndenting() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		targetDefinition.setOS("test_os");
		ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, actualOutput);
		confirmMatch(targetDefinition, "IndentingTestCaseTarget.txt");
	}

	@Test
	public void testCommentsAndWhitespacePersists() throws Exception {
		InputStream inputStream = FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/PersistTestCaseTarget.txt").openStream();
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		TargetDefinitionPersistenceHelper.initFromXML(targetDefinition, inputStream);
		confirmMatch(targetDefinition, "PersistTestCaseTarget.txt");
	}

	@Test
	public void testContainerContentsAreSet() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		addLocationsToDefinition(targetDefinition);
		confirmMatch(targetDefinition, "ContainerContentsTestCaseTarget.txt");
	}

	@Test
	public void testMultipleContainersWithSameRepoPersist() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();

		IUBundleContainer siteContainer1 = (IUBundleContainer) service.newIULocation(new String[] { "unit1", "unit2" },
				new String[] { "1.0", "2.0" }, new URI[] { new URI("TESTURI") }, IUBundleContainer.INCLUDE_REQUIRED);
		IUBundleContainer siteContainer2 = (IUBundleContainer) service.newIULocation(new IInstallableUnit[] {},
				new URI[] { new URI("TESTURI") }, IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS);

		targetDefinition.setTargetLocations(new ITargetLocation[] { siteContainer1, siteContainer2 });
		confirmMatch(targetDefinition, "MultipleContainersSameRepoTestCaseTarget.txt");
	}

	@Test
	public void testITargetLocationExtensionSerialization() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();

		targetDefinition.setTargetLocations(new ITargetLocation[] { new TestTargetLocation() });
		confirmMatch(targetDefinition, "ITargetLocationExtensionTestCaseTarget.txt");
	}

	public static void assertEqualStringIgnoreDelim(String actual, String expected) throws IOException {
		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}
	private void confirmMatch(ITargetDefinition targetDefinition, String expectedDefinitionPath) throws Exception {
		try (Scanner s = new Scanner(FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/" + expectedDefinitionPath).openStream()).useDelimiter("\\A")) {
			String result = s.hasNext() ? s.next() : "";
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors");
			boolean spacesForTabs = preferences.getBoolean("spacesForTabs", false);

			if (spacesForTabs) {
				char[] chars = new char[preferences.getInt("tabWidth", 4)];
				Arrays.fill(chars, ' ');
				result.replace("\t", new String(chars));
			}
			ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
			TargetDefinitionPersistenceHelper.persistXML(targetDefinition, actualOutput);

			assertEqualStringIgnoreDelim(result, actualOutput.toString(StandardCharsets.UTF_8.toString()));
		} catch (IOException e) {
		}
	}

	private void addLocationsToDefinition(ITargetDefinition targetDefinition) throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		// Site bundle containers with different settings
		// Directory container
		ITargetLocation dirContainer = service.newDirectoryLocation("/test/path/to/eclipse/plugins");
		// Profile container with specific config area
		ITargetLocation profileContainer = service.newProfileLocation("/test/path/to/eclipse/",
				"/test/path/to/configuration/location/");
		// Feature container with specific version
		ITargetLocation featureContainer = service.newFeatureLocation("${eclipse_home}", "org.eclipse.test", "1.2.3");
		// Profile container restricted to just two bundles
		ITargetLocation restrictedProfileContainer = service.newProfileLocation("/test/path/to/eclipse/", null);
		IUBundleContainer siteContainer = (IUBundleContainer) service.newIULocation(new String[] { "unit1", "unit2" },
				new String[] { "1.0", "2.0" }, new URI[] { new URI("TESTURI"), new URI("TESTURI2") },
				IUBundleContainer.INCLUDE_REQUIRED);

		NameVersionDescriptor[] restrictions = new NameVersionDescriptor[] {
				new NameVersionDescriptor("org.eclipse.test1", null),
				new NameVersionDescriptor("org.eclipse.test2", null) };
		targetDefinition.setIncluded(restrictions);
		targetDefinition.setTargetLocations(new ITargetLocation[] { dirContainer, profileContainer, featureContainer,
				restrictedProfileContainer, siteContainer });
	}
}