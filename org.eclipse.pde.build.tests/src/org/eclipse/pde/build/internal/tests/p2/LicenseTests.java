/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.build.internal.tests.p2;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.pde.build.internal.tests.Utils;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeFeatureFactory;

public class LicenseTests extends P2TestCase {
	private static final int URL_EMPTY = 1;
	private static final int URL_NON_EMPTY = 2;
	private static final int URL_NONE = 0;
	private static final int DESC_EMPTY = 100;
	private static final int DESC_NON_EMPTY = 101;

	public void testLicenseFeatureOldP2() throws Exception {
		IFolder buildFolder = newTest("testLicenseFeatureOldP2", "licenseFeature1");
		IFolder repo = Utils.createFolder(buildFolder, "repo");
		String repoLocation = "file:" + repo.getLocation().toOSString();

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		properties.put("generate.p2.metadata", "true");
		properties.put("p2.metadata.repo", repoLocation);
		properties.put("p2.artifact.repo", repoLocation);
		properties.put("p2.publish.artifacts", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IMetadataRepository metadata = loadMetadataRepository(repoLocation);
		IArtifactRepository artifacts = loadArtifactRepository(repoLocation);

		IInstallableUnit iu = getIU(metadata, "F1.feature.group");
		License actualLicense = (License) iu.getLicenses().toArray()[0];

		// Check that root file ends up as an artifact
		IInstallableUnit rootFileIU = getIU(metadata, "org.eclipse.rootfiles.ANY.ANY.ANY");
		IArtifactKey rootFileKey = (IArtifactKey) rootFileIU.getArtifacts().toArray()[0];
		File actualRootFile = ((IFileArtifactRepository) artifacts).getArtifactFile(rootFileKey);

		Set entries = new HashSet();
		entries.add("root.html");
		assertZipContents(actualRootFile, entries);

		// Check that all expected files are in zip
		entries = new HashSet();
		entries.add("feature.properties");
		entries.add("feature.xml");
		entries.add("feature_fr.properties");
		entries.add("license.html");
		entries.add("license_fr.html");

		assertZipContents(buildFolder, "repo/features/F1_1.0.0.jar", entries);

		// Check that feature.properties contains all original properties PLUS properties from license
		IFile licensePropertyFile = buildFolder.getFile("features/L1/feature.properties");
		Properties licenseProperties = Utils.loadProperties(licensePropertyFile);

		IFile originalPropertyFile = buildFolder.getFile("features/F1/feature.properties");
		Properties originalProperties = Utils.loadProperties(originalPropertyFile);

		IFile actualPropertiesFile = buildFolder.getFile("checkProperties");
		Utils.extractFromZip(buildFolder, "I.TestBuild/F1-TestBuild.zip", "eclipse/features/F1_1.0.0/feature.properties", actualPropertiesFile);
		Properties actualProperties = Utils.loadProperties(actualPropertiesFile);

		assertEquals("Result feature.properties has incorrect number of properties", originalProperties.size() + 2, actualProperties.size());

		Enumeration keys = originalProperties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			assertEquals(originalProperties.getProperty(key, "originalMissing"), actualProperties.getProperty(key));
		}

		keys = licenseProperties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			assertEquals(licenseProperties.getProperty(key, "originalMissing"), actualProperties.getProperty(key));
		}

		// Check that license from L1 ends up int he meta data for the feature
		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();

		IFile licenseFeatureFile = buildFolder.getFile("features/L1/feature.xml");
		BuildTimeFeature licenseFeature = factory.parseBuildFeature(licenseFeatureFile.getLocationURI().toURL());

		IFile originalFeatureFile = buildFolder.getFile("features/F1/feature.xml");
		BuildTimeFeature originalFeature = factory.parseBuildFeature(originalFeatureFile.getLocationURI().toURL());

		assertEquals(licenseFeature.getLicenseURL(), URIUtil.toUnencodedString(actualLicense.getLocation()));
		assertEquals(licenseFeature.getLicense(), actualLicense.getBody());

		// Check that license elements in feature.xml where changed.
		IFile actualFeatureFile = buildFolder.getFile("checkFeature.xml");
		Utils.extractFromZip(buildFolder, "I.TestBuild/F1-TestBuild.zip", "eclipse/features/F1_1.0.0/feature.xml", actualFeatureFile);
		BuildTimeFeature actualFeature = factory.parseBuildFeature(actualFeatureFile.getLocationURI().toURL());

		assertNotNull(actualFeature.getLicense());
		assertEquals(licenseFeature.getLicense(), actualFeature.getLicense());

		assertNotNull(actualFeature.getLicenseURL());
		assertEquals(licenseFeature.getLicenseURL(), actualFeature.getLicenseURL());

		assertEquals(originalFeature.getId(), actualFeature.getId());
		assertEquals(originalFeature.getDescription(), actualFeature.getDescription());
		assertEquals(originalFeature.getCopyright(), actualFeature.getCopyright());
	}

	// Test various combinations of empty and non-empty license and payload values
	public void testEmptyCombosPDE() throws Exception {

		// Prepare
		IFolder buildFolder = newTest("testEmptyCombosPDE", "emptyLicenseTests");
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		properties.put("baseLocation", "");
		Utils.storeBuildProperties(buildFolder, properties);

		// Test URL combinations
		for (int licenseState = 0; licenseState < 3; licenseState++) {
			for (int payloadState = 0; payloadState < 3; payloadState++) {
				copyFeature(buildFolder, "L1", licenseState);
				copyFeature(buildFolder, "F1", payloadState);
				runBuild(buildFolder);
				checkBuiltFeature(buildFolder, getStateMessage(licenseState, payloadState));
				new File(buildFolder.getLocation().toFile(), "I.TestBuild/F1-TestBuild.zip").delete();
			}
		}

		// Test Description combinations
		for (int licenseState = 100; licenseState < 102; licenseState++) {
			for (int payloadState = 0; payloadState < 3; payloadState++) {
				copyFeature(buildFolder, "L1", licenseState);
				copyFeature(buildFolder, "F1", payloadState);
				runBuild(buildFolder);
				checkBuiltFeature(buildFolder, getStateMessage(licenseState, payloadState));
				new File(buildFolder.getLocation().toFile(), "I.TestBuild/F1-TestBuild.zip").delete();
			}
		}
	}

	public void testRootUsedWithNoLicenseRefPDE() throws Exception {
		boolean expectedExceptionCaught = false;

		IFolder buildFolder = newTest("testRootUsedWithNoLicenseRefPDE", "rootNoRef");
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		Utils.storeBuildProperties(buildFolder, properties);
		try {
			runBuild(buildFolder);
		} catch (Exception e) {
			assertTrue("Build throws wrong exception", e.getMessage().indexOf("uses 'license:' root keyword but does not reference a license feature") != -1);
			expectedExceptionCaught = true;
		}

		assertTrue("Build should have failed", expectedExceptionCaught);
	}

	public void testLicenseFeaturePDE() throws Exception {
		IFolder buildFolder = newTest("testLicenseFeaturePDE", "licenseFeature1");
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		// Check that all expected files are in zip
		Set entries = new HashSet();
		entries.add("eclipse/features/F1_1.0.0/feature.properties");
		entries.add("eclipse/features/F1_1.0.0/feature.xml");
		entries.add("eclipse/features/F1_1.0.0/feature_fr.properties");
		entries.add("eclipse/features/F1_1.0.0/license.html");
		entries.add("eclipse/features/F1_1.0.0/license_fr.html");
		entries.add("eclipse/root.html");

		assertZipContents(buildFolder, "I.TestBuild/F1-TestBuild.zip", entries);

		// Check that feature.properties contains all original properties PLUS properties from license
		IFile licensePropertyFile = buildFolder.getFile("features/L1/feature.properties");
		Properties licenseProperties = Utils.loadProperties(licensePropertyFile);

		IFile originalPropertyFile = buildFolder.getFile("features/F1/feature.properties");
		Properties originalProperties = Utils.loadProperties(originalPropertyFile);

		IFile actualPropertiesFile = buildFolder.getFile("checkProperties");
		Utils.extractFromZip(buildFolder, "I.TestBuild/F1-TestBuild.zip", "eclipse/features/F1_1.0.0/feature.properties", actualPropertiesFile);
		Properties actualProperties = Utils.loadProperties(actualPropertiesFile);

		assertEquals("Result feature.properties has incorrect number of properties", originalProperties.size() + 2, actualProperties.size());

		Enumeration keys = originalProperties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			assertEquals(originalProperties.getProperty(key, "originalMissing"), actualProperties.getProperty(key));
		}

		keys = licenseProperties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			assertEquals(licenseProperties.getProperty(key, "originalMissing"), actualProperties.getProperty(key));
		}

		checkBuiltFeature(buildFolder, "");
	}

	public void testP2Gathering() throws Exception {
		IFolder buildFolder = newTest("licenseP2Gathering", "licenseFeature1");

		Utils.writeBuffer(buildFolder.getFile("features/L1/license.html"), new StringBuffer("I'm a license\n"));
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml,feature.properties,license.html");
		Utils.storeBuildProperties(buildFolder.getFolder("features/L1"), properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		properties.put("baseLocation", "");
		properties.put("p2.gathering", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("license.html");
		assertZipContents(buildFolder, "buildRepo/features/F1_1.0.0.jar", entries);

		//also try using the gather task directly
		IFile testXML = buildFolder.getFile("test.xml");
		IFolder repo2 = Utils.createFolder(buildFolder, "repo2");
		StringBuffer test = new StringBuffer();
		test.append("<project default=\"publish\">                                                                    \n");
		test.append("  <target name=\"publish\">                                                                      \n");
		test.append("    <eclipse.gatherFeature metadataRepository=\"${repo}\" artifactRepository=\"${repo}\"         \n");
		test.append("                        buildResultFolder=\"${folder}\" baseDirectory=\"${folder}\"              \n");
		test.append("                        licenseDirectory=\"${license}\" />                                       \n");
		test.append("  </target>                                                                                     \n");
		test.append("</project>                                                                                      \n");
		Utils.writeBuffer(testXML, test);

		properties = new Properties();
		properties.put("repo", URIUtil.toUnencodedString(repo2.getLocationURI()));
		properties.put("folder", buildFolder.getFolder("features/F1").getLocation().toString());
		properties.put("license", buildFolder.getFolder("features/L1").getLocation().toString());
		runAntScript(testXML.getLocation().toOSString(), new String[] {"publish"}, buildFolder.getLocation().toOSString(), properties);

		entries = new HashSet();
		entries.add("license.html");
		assertZipContents(repo2, "features/F1_1.0.0.jar", entries);
	}

	public void testP2Gathering_Custom() throws Exception {
		IFolder buildFolder = newTest("licenseP2GatheringCustom", "licenseFeature1");

		Utils.writeBuffer(buildFolder.getFile("features/L1/rootFile.html"), new StringBuffer("I'm a license\n"));
		Utils.writeBuffer(buildFolder.getFile("features/L1/sub/license.html"), new StringBuffer("I'm a license\n"));
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml,feature.properties,sub/license.html");
		Utils.storeBuildProperties(buildFolder.getFolder("features/L1"), properties);

		IFolder f = buildFolder.getFolder("features/F1");
		properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		properties.put("customBuildCallbacks", "true");
		properties.put("root", "license:file:rootFile.html");
		Utils.storeBuildProperties(f, properties);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<project name=\"customGather\" default=\"noDefault\">			\n");
		buffer.append("   <target name=\"pre.gather.bin.parts\">						\n");
		buffer.append("      <concat destfile=\"${feature.directory}/a.txt\">			\n");
		buffer.append("        Mary had a little lamb.									\n");
		buffer.append("      </concat>													\n");
		buffer.append("   </target>														\n");
		buffer.append("</project>														\n");
		Utils.writeBuffer(f.getFile("customBuildCallbacks.xml"), buffer);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		properties.put("baseLocation", "");
		properties.put("p2.gathering", "true");

		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("sub/license.html");
		entries.add("a.txt");
		assertZipContents(buildFolder, "buildRepo/features/F1_1.0.0.jar", entries);

		entries = new HashSet();
		entries.add("rootFile.html");
		assertZipContents(buildFolder, "buildRepo/binary/F1_root_1.0.0", entries);
	}

	public void testBinaryLicense() throws Exception {
		IFolder buildFolder = newTest("binaryLicense", "licenseFeature1");

		Utils.writeBuffer(buildFolder.getFile("features/L1/META-INF/MANIFEST.MF"), new StringBuffer("manifest\n"));
		Utils.writeBuffer(buildFolder.getFile("features/L1/META-INF/ECLIPSEF.RSA"), new StringBuffer("rsa\n"));
		Utils.writeBuffer(buildFolder.getFile("features/L1/sub/license.html"), new StringBuffer("I'm a license\n"));
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml,feature.properties,sub/license.html, META-INF/");
		Utils.storeBuildProperties(buildFolder.getFolder("features/L1"), properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "L1");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);

		IFolder build2 = Utils.createFolder(buildFolder, "build2");
		Utils.copy(buildFolder.getFolder("features/F1").getLocation().toFile(), build2.getFolder("features/F1").getLocation().toFile());

		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("topLevelElementId", "F1");
		properties.put("baseLocation", "");
		properties.put("pluginPath", buildFolder.getFolder("tmp/eclipse").getLocation().toString());
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(build2, properties);
		runBuild(build2);

		assertResourceFile(build2, "tmp/eclipse/features/F1_1.0.0/sub/license.html");
		assertFalse(build2.getFolder("tmp/eclipse/features/F1_1.0.0/META-INF").exists());

		IFolder build3 = Utils.createFolder(buildFolder, "build3");
		Utils.copy(buildFolder.getFolder("features/F1").getLocation().toFile(), build3.getFolder("features/F1").getLocation().toFile());

		properties = BuildConfiguration.getBuilderProperties(build3);
		properties.put("topLevelElementId", "F1");
		properties.put("baseLocation", "");
		properties.put("pluginPath", buildFolder.getFolder("tmp/eclipse").getLocation().toString());
		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(build3, properties);
		runBuild(build3);

		Set entries = new HashSet();
		entries.add("sub/license.html");
		assertZipContents(build3, "buildRepo/features/F1_1.0.0.jar", entries);
		assertResourceFile(build2, "tmp/eclipse/features/F1_1.0.0/sub/license.html");
		assertFalse(build2.getFolder("tmp/eclipse/features/F1_1.0.0/META-INF").exists());
		assertResourceFile(build3, "buildRepo/features/F1_1.0.0.jar");
		assertFalse(Utils.extractFromZip(build3, "buildRepo/features/F1_1.0.0.jar", "META-INF/ECLIPSEF.RSA", build3.getFile("rsa.txt")));
	}

	private String getStateMessage(int state) {
		switch (state) {
			case URL_NONE :
				return "No URL";
			case URL_EMPTY :
				return "URL Empty";
			case URL_NON_EMPTY :
				return "URL Not Empty";
			case DESC_EMPTY :
				return "Description Empty";
			case DESC_NON_EMPTY :
				return "Description Not Empty";
			default :
				return "unknown";
		}
	}

	private String getStateMessage(int licenseState, int payloadState) {
		String lState = getStateMessage(licenseState);
		String pState = getStateMessage(payloadState);

		return "License State: " + lState + " Payload State: " + pState + ": ";
	}

	private void checkBuiltFeature(IFolder buildFolder, String errorMessage) throws Exception {
		// Check that license elements in feature.xml where changed.
		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();

		//IFile actualFeatureFile = buildFolder.getFile("tmp/eclipse/features/F1_1.0.0/feature.xml");//buildFolder.getFile("checkFeature.xml");
		IFile actualFeatureFile = buildFolder.getFile("checkFeature.xml");
		assertTrue(Utils.extractFromZip(buildFolder, "I.TestBuild/F1-TestBuild.zip", "eclipse/features/F1_1.0.0/feature.xml", actualFeatureFile));
		BuildTimeFeature actualFeature = factory.parseBuildFeature(actualFeatureFile.getLocationURI().toURL());

		IFile licenseFeatureFile = buildFolder.getFile("features/L1/feature.xml");
		BuildTimeFeature licenseFeature = factory.parseBuildFeature(licenseFeatureFile.getLocationURI().toURL());

		IFile originalFeatureFile = buildFolder.getFile("features/F1/feature.xml");
		BuildTimeFeature originalFeature = factory.parseBuildFeature(originalFeatureFile.getLocationURI().toURL());

		assertNotNull(errorMessage + "license was null", actualFeature.getLicense());
		assertEquals(errorMessage + "license text not equal", licenseFeature.getLicense(), actualFeature.getLicense());

		assertNotNull(errorMessage + "license url was null", actualFeature.getLicenseURL());
		assertEquals(errorMessage + "license url not equal", licenseFeature.getLicenseURL(), actualFeature.getLicenseURL());

		assertEquals(errorMessage + "feature ID corrupted", originalFeature.getId(), actualFeature.getId());
		assertEquals(errorMessage + "feature description corrupted", originalFeature.getDescription(), actualFeature.getDescription());
		assertEquals(errorMessage + "feature copyright corrupted", originalFeature.getCopyright(), actualFeature.getCopyright());
	}

	private void copyFeature(IFolder buildFolder, String featureID, int state) throws CoreException {
		String sourceFileName = null;

		switch (state) {
			case URL_EMPTY :
				sourceFileName = "emptyLicenseURLFeature.xml";
				break;
			case URL_NON_EMPTY :
				sourceFileName = "nonEmptyLicenseURLFeature.xml";
				break;
			case URL_NONE :
				sourceFileName = "noLicenseURLFeature.xml";
				break;
			case DESC_EMPTY :
				sourceFileName = "emptyLicenseDescFeature.xml";
				break;
			case DESC_NON_EMPTY :
				sourceFileName = "nonEmptyLicenseDescFeature.xml";
				break;
		}

		IFile source = buildFolder.getFile("features/" + featureID + "/" + sourceFileName);
		IFile dest = buildFolder.getFile("features/" + featureID + "/feature.xml");
		if (dest.exists()) {
			dest.delete(true, null);
		}
		source.copy(dest.getFullPath(), true, null);
	}

	public void testBug338835_MissingLicenseSection() throws Exception {
		IFolder buildFolder = newTest("338835");
		IFolder featureFolder = Utils.createFolder(buildFolder, "feature");
		IFolder licenseFolder = Utils.createFolder(buildFolder, "license");

		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"org.eclipse.ptp\" version=\"5.0.0.qualifier\"								\n");
		buffer.append(" license-feature=\"org.eclipse.ptp.license\" license-feature-version=\"1.0.0.qualifier\">\n");
		buffer.append("	<description url=\"http://eclipse.org/ptp\">%description</description> 					\n");
		buffer.append("	<copyright>%copyright</copyright>  														\n");
		buffer.append("	<url> 																					\n");
		buffer.append("		<update label=\"%updateSiteName\" url=\"http://download.eclipse.org/updates\"/> 	\n");
		buffer.append("	</url> 																					\n");
		buffer.append("</feature> 																				\n");
		Utils.writeBuffer(featureFolder.getFile("feature.xml"), buffer);

		buffer.append("<feature id=\"license\" version=\"5.0.0.qualifier\" >\n");
		buffer.append("	<license url=\"http://eclipse.org/license.html\"> 	\n");
		buffer.append("		This is legal stuff 							\n");
		buffer.append("	</license> 											\n");
		buffer.append("</feature> 											\n");
		Utils.writeBuffer(licenseFolder.getFile("feature.xml"), buffer);

		buffer = new StringBuffer();
		buffer.append("<project name=\"build\" basedir=\".\" >														\n");
		buffer.append("	<target name=\"test\">																	 	\n");
		buffer.append("		<eclipse.licenseReplacer featureFilePath=\"" + featureFolder.getLocation().toOSString() + "\"	\n");
		buffer.append("			licenseFilePath=\"" + licenseFolder.getLocation().toOSString() + "\" /> 					\n");
		buffer.append("	</target>																					\n");
		buffer.append("</project>																					\n");
		IFile buildXml = buildFolder.getFile("build.xml");
		Utils.writeBuffer(buildXml, buffer);

		runAntScript(buildXml.getLocation().toOSString(), new String[] {"test"}, buildFolder.getLocation().toOSString(), null);
		BuildTimeFeature feature = new BuildTimeFeatureFactory().parseBuildFeature(featureFolder.getFile("feature.xml").getLocationURI().toURL());

		assertEquals(feature.getLicense().trim(), "This is legal stuff");
	}
}
