/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.runtime.FileLocator;
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
	private static final String TEST_FILE_LINE_SEPERATOR = "\n";

	@Test
	public void testSettingNullPersists() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		targetDefinition.setName("test");
		tempFile = File.createTempFile("targetDefinition", null);
		ITextFileBuffer buffer = getTextFileBufferFromFile(tempFile);
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, buffer);
		String expectedOutput = readFile(tempFile.toPath(), StandardCharsets.UTF_8);

		targetDefinition.setProgramArguments(null);
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, buffer);
		String actualOutput = readFile(tempFile.toPath(), StandardCharsets.UTF_8);
		assertEquals(expectedOutput, actualOutput);
	}

	@Test
	public void testIndenting() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		targetDefinition.setOS("test_os");
		confirmMatch(targetDefinition, "IndentingTestCaseTarget.txt");
	}

	@Test
	public void testCommentsAndWhitespacePersists() throws Exception {
		URL url = FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/PersistTestCaseTarget.txt");
		File inputFile = new File(FileLocator.toFileURL(url).getFile());
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		ITextFileBuffer buffer = getTextFileBufferFromFile(inputFile);
		TargetDefinitionPersistenceHelper.initFromXML(targetDefinition, buffer);
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
		String lineSeparator = System.getProperty("line.separator");
		boolean requireReplaceLineSeparator = !lineSeparator.equals(TEST_FILE_LINE_SEPERATOR);
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
			tempFile = File.createTempFile("targetDefinition", null);
			ITextFileBuffer buffer = getTextFileBufferFromFile(tempFile);
			TargetDefinitionPersistenceHelper.persistXML(targetDefinition, buffer);
			String fileContent = readFile(tempFile.toPath(), StandardCharsets.UTF_8);
			if (requireReplaceLineSeparator) {
				fileContent = fileContent.replace(lineSeparator, TEST_FILE_LINE_SEPERATOR);
			}
			assertEquals(result, fileContent);
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

	static String readFile(Path path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(path);
		return new String(encoded, encoding);
	}
}