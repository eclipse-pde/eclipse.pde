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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;

/**
 * NOTE:  To run some of these tests, you must have the delta pack installed in your target.  Any 
 * test that calls {@link Utils#findDeltaPack()} will fail without the delta pack being available
 */
public class AssembleTests extends PDETestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AssembleTests.class.getName());

		//add all the normal tests
		suite.addTestSuite(AssembleTests.class);

		//If running the intermittent tests:
		//		if (System.getProperties().get("pde.build.intermittent") != null) {
		//		}
		return suite;
	}

	public void testCustomAssembly() throws Exception {
		IFolder buildFolder = newTest("customAssembly");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"org.eclipse.swt;unpack=\"false\"", "org.eclipse.swt.win32.win32.x86;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86\""});

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!delta.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", delta.getAbsolutePath());
		buildProperties.put("configs", "win32, win32, x86");

		Utils.storeBuildProperties(buildFolder, buildProperties);
		Utils.generateAllElements(buildFolder, "sdk");
		runBuild(buildFolder);

		String buildLocation = buildFolder.getLocation().toOSString();
		String[] log = new String[] {"post.gather.bin.parts", "eclipse.base: " + buildLocation + "/tmp/eclipse", "post.jarUp", "plugins: " + buildLocation + "/tmp/eclipse/plugins", "features: " + buildLocation + "/tmp/eclipse/features", "pre.archive", "rootFolder: " + buildLocation + "/tmp/eclipse/win32.win32.x86/eclipse", "archiveFullPath: " + buildLocation + "/I.TestBuild/eclipse-win32.win32.x86.zip"};
		assertLogContainsLines(buildFolder.getFile("log.log"), log);
	}

	public void testBug179612_default() throws Exception {
		IFolder buildFolder = newTest("179612");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"org.eclipse.swt;unpack=\"false\"", "org.eclipse.swt.win32.win32.x86;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86\"", "org.eclipse.swt.gtk.linux.x86;unpack=\"false\";os=\"linux\";ws=\"gtk\";arch=\"x86\""});

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!delta.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", delta.getAbsolutePath());
		buildProperties.put("configs", "*,*,* & win32, win32, x86 & linux, gtk, x86");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild.zip");
		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild-linux.gtk.x86.zip");
		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild-win32.win32.x86.zip");
	}

	public void testBug179612_custom() throws Exception {
		//we have a custom allElements.xml coming from the resources folder
		IFolder buildFolder = newTest("179612_custom");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"org.eclipse.swt;unpack=\"false\"", "org.eclipse.swt.win32.win32.x86;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86\"", "org.eclipse.swt.gtk.linux.x86;unpack=\"false\";os=\"linux\";ws=\"gtk\";arch=\"x86\""});

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!delta.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", delta.getAbsolutePath());
		buildProperties.put("configs", "*,*,* & win32, win32, x86 & linux, gtk, x86");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		String[] log = new String[] {"preAssemble", "defaultAssemble", "assemble.sdk.win32.win32.x86", "defaultAssemble", "postAssemble", "prePackage", "defaultAssemble", "assemble.sdk.win32.win32.x86", "defaultAssemble", "postPackage"};
		assertLogContainsLines(buildFolder.getFile("log.log"), log);

		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild.zip");
		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild-linux.gtk.x86.zip");
		assertResourceFile(buildFolder, "I.TestBuild/MyCustomName.zip");
	}

	public void testBug196754() throws Exception {
		IFolder buildFolder = newTest("196754");

		// pde.build and equinox.launcher.win32.win32.x86 exist as signed folders in the base location,
		// jar them up in the build and assert they still verify
		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"org.eclipse.pde.build;unpack=\"false\"", "org.eclipse.equinox.launcher.win32.win32.x86;unpack=\"false\""});

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("archivesFormat", "*, *, * - folder");
		if (!delta.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", delta.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, buildProperties);
		Utils.generateAllElements(buildFolder, "sdk");

		runBuild(buildFolder);

		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		File[] plugins = buildFolder.getFolder("tmp/eclipse/plugins").getLocation().toFile().listFiles();
		for (int i = 0; i < plugins.length; i++) {
			assertJarVerifies(plugins[i]);
		}
	}

	public void testBug211605() throws Exception {
		IFolder buildFolder = newTest("211605");

		File delta = Utils.findDeltaPack();
		assertNotNull(delta);

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"org.eclipse.swt;unpack=\"false\"", "org.eclipse.swt.win32.win32.x86;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86\"", "org.eclipse.swt.gtk.linux.x86;unpack=\"false\";os=\"linux\";ws=\"gtk\";arch=\"x86\""});

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!delta.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", delta.getAbsolutePath());
		buildProperties.put("configs", "win32, win32, x86 & linux, gtk, x86");
		buildProperties.put("archivesFormat", "group,group,group-folder");
		buildProperties.put("groupConfigurations", "true");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		File resultFolder = new File(buildFolder.getLocation().toFile(), "tmp/eclipse/plugins");
		assertEquals(resultFolder.list().length, 3);
	}

	public void testBug255824() throws Exception {
		IFolder buildFolder = newTest("255824");

		IFolder a = Utils.createFolder(buildFolder, "plugins/A");
		Utils.generateBundle(a, "A");
		Utils.writeBuffer(a.getFile("src/a.java"), new StringBuffer("class A {}"));

		IFolder b = Utils.createFolder(buildFolder, "plugins/B_1.0.0");
		Utils.generateBundle(b, "B");
		b.getFile("build.properties").delete(true, null);

		Utils.generateFeature(buildFolder, "F", null, new String[] {"A;unpack=false", "B"});

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "F");
		buildProperties.put("archivePrefix", "eclipse");
		buildProperties.put("collectingFolder", "e4");
		buildProperties.put("baseLocation", "");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/plugins/A_1.0.0.jar");
		entries.add("eclipse/plugins/B_1.0.0/META-INF/MANIFEST.MF");
		assertZipContents(buildFolder, "I.TestBuild/F-TestBuild.zip", entries);

		buildProperties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runBuild(buildFolder);
		assertResourceFile(buildFolder, "tmp/e4/plugins/A_1.0.0.jar");
		assertResourceFile(buildFolder, "tmp/e4/plugins/B_1.0.0/META-INF/MANIFEST.MF");
	}

	public void testPackager_bug315710() throws Exception {
		IFolder buildFolder = newTest("315710");

		Utils.generateFeature(buildFolder, "F1", null, new String[] {"org.eclipse.swt;unpack=\"false\""});
		Utils.generateFeature(buildFolder, "F2", null, new String[] {"org.eclipse.osgi;unpack=\"false\""});
		Utils.writeBuffer(buildFolder.getFile("features/F1/notice.html"), new StringBuffer("be nice to clowns\n"));
		Utils.writeBuffer(buildFolder.getFile("features/F1/build.properties"), new StringBuffer("bin.includes=feature.xml\nroot=file:notice.html\n"));
		Utils.writeBuffer(buildFolder.getFile("features/F2/build.properties"), new StringBuffer("bin.includes=feature.xml\n"));

		StringBuffer customBuffer = new StringBuffer();
		customBuffer.append("<project name=\"custom\" default=\"noDefault\">										\n");
		customBuffer.append("   <import file=\"${eclipse.pdebuild.templates}/headless-build/allElements.xml\"/>	\n");
		customBuffer.append("   <target name=\"allElementsDelegator\">												\n");
		customBuffer.append("      <ant antfile=\"${genericTargets}\" target=\"${target}\">							\n");
		customBuffer.append("         <property name=\"type\" value=\"feature\" />									\n");
		customBuffer.append("         <property name=\"id\" value=\"F1\" />											\n");
		customBuffer.append("      </ant>																			\n");
		customBuffer.append("      <ant antfile=\"${genericTargets}\" target=\"${target}\">							\n");
		customBuffer.append("         <property name=\"type\" value=\"feature\" />									\n");
		customBuffer.append("         <property name=\"id\" value=\"F2\" />											\n");
		customBuffer.append("      </ant>																			\n");
		customBuffer.append("   </target>																			\n");
		customBuffer.append("</project>																				\n");
		Utils.writeBuffer(buildFolder.getFile("allElements.xml"), customBuffer);

		Utils.storeBuildProperties(buildFolder, BuildConfiguration.getBuilderProperties(buildFolder));
		runBuild(buildFolder);

		IFile f1zip = buildFolder.getFile("I.TestBuild/F1-TestBuild.zip");
		IFile f2zip = buildFolder.getFile("I.TestBuild/F2-TestBuild.zip");

		assertResourceFile(f1zip);
		assertResourceFile(f2zip);

		IFolder packageFolder = Utils.createFolder(buildFolder, "packager");

		Properties properties = new Properties();
		properties.put("F1-TestBuild.zip", URIUtil.toUnencodedString(buildFolder.getFolder("I.TestBuild").getLocationURI()) + "/|||stuff|components");
		properties.put("F2-TestBuild.zip", URIUtil.toUnencodedString(buildFolder.getFolder("I.TestBuild").getLocationURI()) + "/|||stuff|other");
		Utils.storeProperties(packageFolder.getFile("packager.map"), properties);

		URL templates = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/templates/packager"), null);
		Utils.copy(new File(FileLocator.toFileURL(templates).getPath()), new File(packageFolder.getLocationURI()));
		packageFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

		properties = Utils.loadProperties(packageFolder.getFile("packager.properties"));
		properties.put("baseDirectory", packageFolder.getLocation().toOSString());
		properties.put("featureList", "F1, F2");
		properties.put("componentFilter", "*");
		properties.put("contentFilter", "");
		properties.put("packagerMapURL", URIUtil.toUnencodedString(packageFolder.getFile("packager.map").getLocationURI()));
		properties.put("config", "win32,win32,x86");
		properties.remove("prefilledTarget");
		Utils.storeProperties(packageFolder.getFile("packager.properties"), properties);

		Utils.writeBuffer(packageFolder.getFile("packaging.properties"), new StringBuffer("root=notice.html\n"));

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new Path("/scripts/package.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		properties.clear();
		properties.put("packagingInfo", packageFolder.getLocation().toOSString());
		runAntScript(buildXMLPath, new String[] {"main"}, packageFolder.getLocation().toOSString(), properties);

		properties = Utils.loadProperties(buildFolder.getFile("finalPluginsVersions.properties"));

		Set set = new HashSet();
		set.add("eclipse/notice.html");
		set.add("eclipse/features/F1_1.0.0/feature.xml");
		set.add("eclipse/features/F2_1.0.0/feature.xml");
		set.add("eclipse/plugins/org.eclipse.osgi_" + properties.get("org.eclipse.osgi") + ".jar");
		set.add("eclipse/plugins/org.eclipse.swt_" + properties.get("org.eclipse.swt") + ".jar");
		assertZipContents(packageFolder, "workingPlace/I.MyProduct/MyProduct-win32.win32.win32.zip", set);
	}
}
