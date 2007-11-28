/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.File;
import java.util.Properties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;

public class AssembleTests extends PDETestCase {

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
		String [] log = new String [] {
			"post.gather.bin.parts",
			"eclipse.base: " + buildLocation + "/tmp/eclipse",
			"post.jarUp",
			"plugins: " + buildLocation + "/tmp/eclipse/plugins",
			"features: " + buildLocation + "/tmp/eclipse/features",
			"pre.archive",
			"rootFolder: " + buildLocation + "/tmp/eclipse/win32.win32.x86/eclipse",
			"archiveFullPath: " + buildLocation + "/I.TestBuild/eclipse-win32.win32.x86.zip"
		};
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
		File [] plugins = buildFolder.getFolder("tmp/eclipse/plugins").getLocation().toFile().listFiles();
		for (int i = 0; i < plugins.length; i++) {
			assertJarVerifies(plugins[i]);
		}		
	}
	
}
