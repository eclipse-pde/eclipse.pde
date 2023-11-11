/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;
import org.junit.Test;

public class AssembleTests extends PDETestCase {
	@Test
	public void testCustomAssembly() throws Exception {
		IFolder buildFolder = newTest("customAssembly");

		File executable = Utils.findExecutable();
		assertNotNull(executable);

		Utils.generateFeature(buildFolder, "sdk", null, new String[] { "org.eclipse.swt;unpack=\"false\"",
				"org.eclipse.swt.win32.win32.x86_64;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86_64\"" });

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!executable.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", executable.getAbsolutePath());
		buildProperties.put("configs", "win32, win32, x86_64");

		Utils.storeBuildProperties(buildFolder, buildProperties);
		Utils.generateAllElements(buildFolder, "sdk");
		runBuild(buildFolder);

		String buildLocation = buildFolder.getLocation().toOSString();
		String[] log = new String[] { "post.gather.bin.parts", "eclipse.base: " + buildLocation + "/tmp/eclipse",
				"post.jarUp", "plugins: " + buildLocation + "/tmp/eclipse/plugins",
				"features: " + buildLocation + "/tmp/eclipse/features", "pre.archive",
				"rootFolder: " + buildLocation + "/tmp/eclipse/win32.win32.x86_64/eclipse",
				"archiveFullPath: " + buildLocation + "/I.TestBuild/eclipse-win32.win32.x86_64.zip" };
		assertLogContainsLines(buildFolder.getFile("log.log"), log);
	}

	@Test
	public void testBug179612_default() throws Exception {
		IFolder buildFolder = newTest("179612");

		File executable = Utils.findExecutable();
		assertNotNull(executable);

		Utils.generateFeature(buildFolder, "sdk", null,
				new String[] { "org.eclipse.swt;unpack=\"false\"",
						"org.eclipse.swt.win32.win32.x86_64;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86_64\"",
						"org.eclipse.swt.gtk.linux.x86_64;unpack=\"false\";os=\"linux\";ws=\"gtk\";arch=\"x86_64\"" });

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!executable.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", executable.getAbsolutePath());
		buildProperties.put("configs", "*,*,* & win32, win32, x86_64 & linux, gtk, x86_64");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild.zip");
		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild-linux.gtk.x86_64.zip");
		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild-win32.win32.x86_64.zip");
	}

	@Test
	public void testBug179612_custom() throws Exception {
		// we have a custom allElements.xml coming from the resources folder
		IFolder buildFolder = newTest("179612_custom");

		File executable = Utils.findExecutable();
		assertNotNull(executable);

		Utils.generateFeature(buildFolder, "sdk", null,
				new String[] { "org.eclipse.swt;unpack=\"false\"",
						"org.eclipse.swt.win32.win32.x86_64;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86_64\"",
						"org.eclipse.swt.gtk.linux.x86_64;unpack=\"false\";os=\"linux\";ws=\"gtk\";arch=\"x86_64\"" });

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!executable.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", executable.getAbsolutePath());
		buildProperties.put("configs", "*,*,* & win32, win32, x86_64 & linux, gtk, x86_64");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		String[] log = new String[] { "preAssemble", "defaultAssemble", "assemble.sdk.win32.win32.x86_64",
				"defaultAssemble", "postAssemble", "prePackage", "defaultAssemble", "assemble.sdk.win32.win32.x86_64",
				"defaultAssemble", "postPackage" };
		assertLogContainsLines(buildFolder.getFile("log.log"), log);

		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild.zip");
		assertResourceFile(buildFolder, "I.TestBuild/sdk-TestBuild-linux.gtk.x86_64.zip");
		assertResourceFile(buildFolder, "I.TestBuild/MyCustomName.zip");
	}

	@Test
	public void testBug196754() throws Exception {
		IFolder buildFolder = newTest("196754");

		// pde.build and equinox.launcher.win32.win32.x86 exist as signed folders in the
		// base location,
		// jar them up in the build and assert they still verify
		Utils.generateFeature(buildFolder, "sdk", null, new String[] { "org.eclipse.pde.build;unpack=\"false\"",
				"org.eclipse.equinox.launcher.win32.win32.x86_64;unpack=\"false\"" });

		File executable = Utils.findExecutable();
		assertNotNull(executable);

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("archivesFormat", "*, *, * - folder");
		buildProperties.put("signJars", "false");
		if (!executable.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", executable.getAbsolutePath());
		Utils.storeBuildProperties(buildFolder, buildProperties);
		Utils.generateAllElements(buildFolder, "sdk");

		runBuild(buildFolder);

		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
		File[] plugins = buildFolder.getFolder("tmp/eclipse/plugins").getLocation().toFile().listFiles();
		for (File plugin : plugins) {
			assertJarVerifies(plugin);
		}
	}

	@Test
	public void testBug211605() throws Exception {
		IFolder buildFolder = newTest("211605");

		File executable = Utils.findExecutable();
		assertNotNull(executable);

		Utils.generateFeature(buildFolder, "sdk", null,
				new String[] { "org.eclipse.swt;unpack=\"false\"",
						"org.eclipse.swt.win32.win32.x86_64;unpack=\"false\";os=\"win32\";ws=\"win32\";arch=\"x86_64\"",
						"org.eclipse.swt.gtk.linux.x86_64;unpack=\"false\";os=\"linux\";ws=\"gtk\";arch=\"x86_64\"" });

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "sdk");
		if (!executable.equals(new File((String) buildProperties.get("baseLocation"))))
			buildProperties.put("pluginPath", executable.getAbsolutePath());
		buildProperties.put("configs", "win32, win32, x86_64 & linux, gtk, x86_64");
		buildProperties.put("archivesFormat", "group,group,group-folder");
		buildProperties.put("groupConfigurations", "true");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		File resultFolder = new File(buildFolder.getLocation().toFile(), "tmp/eclipse/plugins");
		assertEquals(resultFolder.list().length, 3);
	}

	@Test
	public void testBug255824() throws Exception {
		IFolder buildFolder = newTest("255824");

		IFolder a = Utils.createFolder(buildFolder, "plugins/A");
		Utils.generateBundle(a, "A");
		Utils.writeBuffer(a.getFile("src/a.java"), new StringBuffer("class A {}"));

		IFolder b = Utils.createFolder(buildFolder, "plugins/B_1.0.0");
		Utils.generateBundle(b, "B");
		b.getFile("build.properties").delete(true, null);

		Utils.generateFeature(buildFolder, "F", null, new String[] { "A;unpack=false", "B" });

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("topLevelElementId", "F");
		buildProperties.put("archivePrefix", "eclipse");
		buildProperties.put("collectingFolder", "e4");
		buildProperties.put("baseLocation", "");

		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		Set<String> entries = new HashSet<>();
		entries.add("eclipse/plugins/A_1.0.0.jar");
		entries.add("eclipse/plugins/B_1.0.0.jar:META-INF/MANIFEST.MF");
		assertZipContents(buildFolder, "I.TestBuild/F-TestBuild.zip", entries);

		buildProperties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runBuild(buildFolder);
		assertResourceFile(buildFolder, "tmp/e4/plugins/A_1.0.0.jar");
		assertResourceFile(buildFolder, "tmp/e4/plugins/B_1.0.0.jar:META-INF/MANIFEST.MF");
	}
}
