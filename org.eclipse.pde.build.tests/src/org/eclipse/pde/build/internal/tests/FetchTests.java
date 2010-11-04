/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

/**
 * These tests are not included in the main test suite unless the "pde.build.includeFetch" system property
 * is defined.  This is because the test machines have firewalls which cause some of the tests to fail.
 */
public class FetchTests extends PDETestCase {

	public void testBug174194() throws Exception {
		IFolder buildFolder = newTest("174194");
		IFile script = buildFolder.getFile("testbuild.xml");

		try {
			runAntScript(script.getLocation().toOSString(), new String[] {"default"}, buildFolder.getLocation().toOSString(), new Properties());
		} catch (Exception e) {
			assertTrue(e.getMessage().endsWith("Could not retrieve feature.xml or build.properties for feature org.eclipse.rcp."));
		}

		assertResourceFile(buildFolder, "log.log");
		assertLogContainsLine(buildFolder.getFile("log.log"), "Could not retrieve feature.xml or build.properties for feature org.eclipse.rcp");
	}

	public void testGetUnpack() throws Exception {
		IFolder buildFolder = newTest("testGetUnpack");

		StringBuffer buffer = new StringBuffer();
		buffer.append("plugin@javax.xml.rpc,1.1.0=GET,http://download.eclipse.org/tools/orbit/downloads/drops/R20090825191606/bundles/javax.xml.rpc_1.1.0.v200905122109.zip,unpack=true\n");
		buffer.append("plugin@com.ibm.icu.base,3.6.0=GET,http://download.eclipse.org/tools/orbit/downloads/drops/R20090825191606/updateSite/plugins/com.ibm.icu.base_3.6.0.v20080530.jar,unpack=true,dest=${buildDirectory}/plugins/com.ibm.icu.base_3.6.0/.zip\n");
		buffer.append("plugin@com.ibm.icu.base,3.6.1=GET,http://download.eclipse.org/tools/orbit/downloads/drops/R20090825191606/updateSite/plugins/com.ibm.icu.base_3.6.1.v20080530.jar,unpack=true\n");
		Utils.writeBuffer(buildFolder.getFile("directory.txt"), buffer);

		Utils.generateFeature(buildFolder, "org.eclipse.pde.build.container.feature", null, new String[] {"javax.xml.rpc", "com.ibm.icu.base;version=3.6.0.qualifier", "com.ibm.icu.base;version=3.6.1.qualifier"});

		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "org.eclipse.pde.build.container.feature");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		assertResourceFile(buildFolder.getFile("plugins/javax.xml.rpc_1.1.0.v200905122109/META-INF/MANIFEST.MF"));
		assertResourceFile(buildFolder.getFile("plugins/com.ibm.icu.base_3.6.0/META-INF/MANIFEST.MF"));
		assertResourceFile(buildFolder.getFile("plugins/com.ibm.icu.base_3.6.1.v20080530/META-INF/MANIFEST.MF"));
	}

	public void testFetchFeature() throws Exception {
		IFolder buildFolder = newTest("fetchFeature");

		StringBuffer buffer = new StringBuffer();
		buffer.append("feature@org.eclipse.cvs=v20090619,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,,org.eclipse.cvs-feature\n");
		buffer.append("plugin@org.eclipse.cvs=v20090520,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,,org.eclipse.sdk-feature/plugins/org.eclipse.cvs\n");
		buffer.append("plugin@org.eclipse.team.cvs.core=I20090430-0408,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,\n");
		buffer.append("plugin@org.eclipse.team.cvs.ssh2=I20090508-2000,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,\n");
		buffer.append("plugin@org.eclipse.team.cvs.ui=I20090521-1750,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,\n");
		Utils.writeBuffer(buildFolder.getFile("directory.txt"), buffer);

		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "org.eclipse.cvs");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		IFile sourceRefsFile = buildFolder.getFile(IPDEBuildConstants.DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR);
		assertResourceFile(sourceRefsFile);
		Properties sourceRefs = Utils.loadProperties(sourceRefsFile);
		assertEquals(sourceRefs.get("org.eclipse.cvs,0.0.0"), "scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.sdk-feature/plugins/org.eclipse.cvs;tag=v20090520");
		assertEquals(sourceRefs.get("org.eclipse.team.cvs.core,0.0.0"), "scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.team.cvs.core;tag=I20090430-0408");
		assertEquals(sourceRefs.get("org.eclipse.team.cvs.ssh2,0.0.0"), "scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.team.cvs.ssh2;tag=I20090508-2000");
	}

	public void testFetch_308696() throws Exception {
		IFolder buildFolder = newTest("308696");

		StringBuffer buffer = new StringBuffer();
		buffer.append("feature@org.eclipse.cvs=v20090619,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,,org.eclipse.cvs-feature\n");
		buffer.append("plugin@org.eclipse.cvs=v20100407,user@dev.eclipse.org:/cvsroot/eclipse,,org.eclipse.sdk-feature/plugins/org.eclipse.cvs\n");
		buffer.append("plugin@org.eclipse.team.cvs.core=I20100310-0800,user@dev.eclipse.org:/cvsroot/eclipse,\n");
		buffer.append("plugin@org.eclipse.team.cvs.ssh2=I20090508-2000,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,\n");
		buffer.append("plugin@org.eclipse.team.cvs.ui=I20090521-1750,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,\n");
		Utils.writeBuffer(buildFolder.getFile("directory.txt"), buffer);

		StringBuffer script = new StringBuffer();
		script.append("<project> 											\n");
		script.append(" <target name=\"fetchElement\">						\n");
		script.append("   <mkdir dir=\"${buildDirectory}/features\"/>		\n");
		script.append("   <mkdir dir=\"${buildDirectory}/plugins\"/>		\n");
		script.append("   <eclipse.fetch									\n");
		script.append("      elements=\"feature@org.eclipse.cvs\"			\n");
		script.append("      buildDirectory=\"${buildDirectory}\"			\n");
		script.append("      directory=\"${buildDirectory}/directory.txt\"	\n");
		//script.append("      baseLocation=\"${baseLocation}\"				\n");
		script.append("   />												\n");
		script.append(" </target>											\n");
		script.append("</project>											\n");
		IFile scriptFile = buildFolder.getFile("script.xml");
		Utils.writeBuffer(scriptFile, script);

		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		//fetchProperties.put("baseLocation", Platform.getInstallLocation().getURL().getPath());
		runAntScript(scriptFile.getLocation().toOSString(), new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		IFile sourceRefsFile = buildFolder.getFile(IPDEBuildConstants.DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR);
		assertResourceFile(sourceRefsFile);
		Properties sourceRefs = Utils.loadProperties(sourceRefsFile);
		assertEquals(sourceRefs.get("org.eclipse.cvs,0.0.0"), "scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.sdk-feature/plugins/org.eclipse.cvs;tag=v20100407");
		assertEquals(sourceRefs.get("org.eclipse.team.cvs.core,0.0.0"), "scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.team.cvs.core;tag=I20100310-0800");

	}

	public void testFetchP2Feature() throws Exception {
		IFolder buildFolder = newTest("p2.fetchFeature");

		StringBuffer buffer = new StringBuffer();
		buffer.append("feature@org.eclipse.cvs=p2IU,id=org.eclipse.cvs.feature.jar,repository=http://eclipsebuildserv/3.7-I-builds/\n");
		buffer.append("plugin@org.eclipse.cvs=p2IU,id=org.eclipse.cvs,repository=http://eclipsebuildserv/3.7-I-builds/\n");
		buffer.append("plugin@org.eclipse.team.cvs.core=p2IU,id=org.eclipse.team.cvs.core,repository=http://eclipsebuildserv/3.7-I-builds/\n");
		buffer.append("plugin@org.eclipse.team.cvs.ssh2=p2IU,id=org.eclipse.team.cvs.ssh2,repository=http://eclipsebuildserv/3.7-I-builds/\n");
		buffer.append("plugin@org.eclipse.team.cvs.ui=p2IU,id=org.eclipse.team.cvs.ui,repository=http://eclipsebuildserv/3.7-I-builds/\n");
		Utils.writeBuffer(buildFolder.getFile("directory.txt"), buffer);

		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("transformedRepoLocation", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "org.eclipse.cvs");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		File features = new File(buildFolder.getLocation().toFile(), "features");
		File plugins = new File(buildFolder.getLocation().toFile(), "plugins");
		assertEquals(features.list().length, 1);
		assertEquals(plugins.list().length, 4);
	}

	public void testFetchLicenseFeature() throws Exception {
		IFolder buildFolder = newTest("license.fetchFeature");

		StringBuffer buffer = new StringBuffer();
		buffer.append("feature@F1=HEAD,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,,pde/build/org.eclipse.pde.build.tests/resources/licenseFeature1/features/F1\n");
		buffer.append("feature@L1=HEAD,:pserver:anonymous@dev.eclipse.org:/cvsroot/eclipse,,pde/build/org.eclipse.pde.build.tests/resources/licenseFeature1/features/L1\n");
		Utils.writeBuffer(buildFolder.getFile("directory.txt"), buffer);

		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "F1");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		File features = new File(buildFolder.getLocation().toFile(), "features");
		assertEquals(2, features.list().length);
	}

	public void testBug248767_2() throws Exception {
		IFolder buildFolder = newTest("248767_2");
		IFolder base = Utils.createFolder(buildFolder, "base");
		IFolder A = Utils.createFolder(buildFolder, "plugins/A");

		Utils.generateFeature(buildFolder, "org.eclipse.pde.build.container.feature", null, new String[] {"org.eclipse.osgi.util", "A"});

		StringBuffer buffer = new StringBuffer("plugin@org.eclipse.osgi.util=v20080303,:pserver:anonymous@dev.eclipse.org:/cvsroot/rt,,org.eclipse.equinox/compendium/bundles/org.eclipse.osgi.util");
		Utils.createFolder(buildFolder, "maps");
		Utils.writeBuffer(buildFolder.getFile("maps/test.map"), buffer);

		Utils.generatePluginBuildProperties(A, null);
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.osgi.util");
		Utils.generateBundleManifest(A, "A", "1.0.0", manifestAdditions);
		buffer = new StringBuffer();
		buffer.append("import org.osgi.util.measurement.Measurement;");
		buffer.append("public class Foo {                           ");
		buffer.append("   public static void main(String[] args) {  ");
		buffer.append("      Measurement m = new Measurement(1.0);  ");
		buffer.append("   }                                         ");
		buffer.append("}                                            ");
		Utils.createFolder(A, "src");
		Utils.writeBuffer(A.getFile("src/Foo.java"), buffer);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "org.eclipse.pde.build.container.feature");
		properties.put("baseLocation", base.getLocation().toOSString());
		properties.put("pluginPath", FileLocator.getBundleFile(Platform.getBundle("org.eclipse.osgi")).getAbsolutePath());
		properties.remove("skipFetch");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug171869_Get() throws Exception {
		IFolder buildFolder = newTest("171869");
		Utils.createFolder(buildFolder, "plugins");

		//org.eclipse.pde.build.container.feature is special in that the fetch won't try
		//to fetch it, and will just fetch everything it includes.
		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "org.eclipse.pde.build.container.feature");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.1.v20080530.jar");
		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.0.v20080530.jar");
	}

	public void testP2Get() throws Exception {
		IFolder buildFolder = newTest("p2.get");
		Utils.createFolder(buildFolder, "plugins");

		// copy over the directory.txt file and make sure that it has the right location for the repository
		URL mapFile = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new Path("/resources/p2.get/directory.txt.template"), null);
		URL repoLocation = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new Path("/resources/repos/1"), null);
		repoLocation = FileLocator.resolve(repoLocation);
		Map replacements = new HashMap();
		replacements.put("repoLocation", repoLocation.toExternalForm());
		Utils.transferAndReplace(mapFile, buildFolder.getFile("directory.txt"), replacements);

		//org.eclipse.pde.build.container.feature is special in that the fetch won't try
		//to fetch it, and will just fetch everything it includes.
		Properties fetchProperties = new Properties();
		fetchProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		fetchProperties.put("transformedRepoLocation", buildFolder.getLocation().toOSString());
		fetchProperties.put("type", "feature");
		fetchProperties.put("id", "org.eclipse.pde.build.container.feature");

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/genericTargets.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"fetchElement"}, buildFolder.getLocation().toOSString(), fetchProperties);

		assertResourceFile(buildFolder, "plugins/aBundle_1.0.0.jar");
	}

	public void testBug306510() throws Exception {
		IFolder buildFolder = newTest("306510");
		Utils.generateFeature(buildFolder, "org.eclipse.pde.build.container.feature", null, new String[] {"org.eclipse.osgi.util;version=3.2.100.vR_qualifier"});

		StringBuffer buffer = new StringBuffer("plugin@org.eclipse.osgi.util,3.2.100.vR_qualifier=v20100108,:pserver:anonymous@dev.eclipse.org:/cvsroot/rt,,org.eclipse.equinox/compendium/bundles/org.eclipse.osgi.util");
		Utils.createFolder(buildFolder, "maps");
		Utils.writeBuffer(buildFolder.getFile("maps/test.map"), buffer);

		StringBuffer customBuffer = new StringBuffer();
		customBuffer.append("<project name=\"custom\" default=\"noDefault\">										\n");
		customBuffer.append("   <import file=\"${eclipse.pdebuild.templates}/headless-build/customTargets.xml\"/>	\n");
		customBuffer.append("   <target name=\"postFetch\">															\n");
		customBuffer.append("      <replace file=\"${buildDirectory}/plugins/org.eclipse.osgi.util_3.2.100.vR_qualifier/META-INF/MANIFEST.MF\"  \n");
		customBuffer.append("               token=\"qualifier\" value=\"vR_qualifier\" /> 							\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("</project>																				\n");
		Utils.writeBuffer(buildFolder.getFile("customTargets.xml"), customBuffer);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "org.eclipse.pde.build.container.feature");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.remove("skipFetch");

		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Manifest m = Utils.loadManifest(buildFolder.getFile("tmp/eclipse/plugins/org.eclipse.osgi.util_3.2.100.vR_v20100108/META-INF/MANIFEST.MF"));
		assertEquals(m.getMainAttributes().getValue("Bundle-Version"), "3.2.100.vR_v20100108");
	}
}
