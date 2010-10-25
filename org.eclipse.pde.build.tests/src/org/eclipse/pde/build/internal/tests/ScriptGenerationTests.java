/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrew Eisenberg - bug 303960 tests
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Path;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.build.internal.tests.ant.AntUtils;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.site.*;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;
import org.osgi.framework.Version;

public class ScriptGenerationTests extends PDETestCase {

	// Test that script generation works when buildDirectory does not contain a plugins subdirectory
	public void testBug147292() throws Exception {
		IFolder buildFolder = newTest("147292");

		String bundleId = "org.eclipse.pde.build.test.147292";
		Utils.generateBundle(buildFolder, bundleId);

		//getScriptGenerationProperties sets buildDirectory to buildFolder by default
		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", bundleId);
		properties.put("baseLocation", buildFolder.getLocation().toOSString());
		generateScripts(buildFolder, properties);

		// test passes if generateScripts did not throw an exception 
		assertResourceFile(buildFolder, "build.xml");
	}

	// Test that the order in which archivesFormat and configInfo are set does not matter
	public void testBug148288() throws Exception {
		IProject buildProject = newTest();

		class MyBuildScriptGenerator extends BuildScriptGenerator {
			public HashMap getArchivesFormat() {
				return super.getArchivesFormat();
			}
		};

		String location = buildProject.getLocation().toOSString();
		MyBuildScriptGenerator generator = new MyBuildScriptGenerator();
		generator.setElements(new String[] {});
		generator.setWorkingDirectory(location);
		BuildTimeSiteFactory.setInstalledBaseSite(location);
		AbstractScriptGenerator.setConfigInfo("win32, win32, x86");
		generator.setArchivesFormat("win32, win32, x86 - antZip");
		generator.generate();

		HashMap map = generator.getArchivesFormat();
		assertEquals(map.size(), 1);
		Config config = (Config) map.keySet().iterator().next();
		assertEquals(map.get(config), "antZip");

		clearStatics();

		generator = new MyBuildScriptGenerator();
		generator.setElements(new String[] {});
		generator.setWorkingDirectory(location);
		BuildTimeSiteFactory.setInstalledBaseSite(location);
		generator.setArchivesFormat("win32, win32, x86 - folder");
		AbstractScriptGenerator.setConfigInfo("win32, win32, x86");
		generator.generate();

		map = generator.getArchivesFormat();
		assertEquals(map.size(), 1);
		config = (Config) map.keySet().iterator().next();
		assertEquals(map.get(config), "folder");
	}

	// Test script generation for bundles using Bundle-RequiredExecutionEnvironment
	// when the state does not contain org.eclipse.osgi
	public void testBug178447() throws Exception {
		IFolder buildFolder = newTest("178447");

		String bundleId = "org.eclipse.pde.build.test.178447";
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Bundle-RequiredExecutionEnvironment"), "J2SE-1.3");
		Utils.generateBundleManifest(buildFolder, bundleId, "1.0.0", manifestAdditions);
		Utils.generatePluginBuildProperties(buildFolder, null);

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", bundleId);
		properties.put("baseLocation", buildFolder.getLocation().toOSString());
		generateScripts(buildFolder, properties);

		// test passes if generateScripts did not throw an exception 
		assertResourceFile(buildFolder, "build.xml");
	}

	// Test the use of customBuildCallbacks.buildpath
	public void testBug183869() throws Exception {
		IFolder buildFolder = newTest("183869");

		Utils.generateAllElements(buildFolder, "a.feature");

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		assertResourceFile(buildFolder, "log.log");
		String[] lines = new String[] {"[echo] Hello Plugin!", "[echo] Hello Feature!"};
		assertLogContainsLines(buildFolder.getFile("log.log"), lines);
	}

	// test platform.xml
	public void testBug183924() throws Exception {
		IFolder buildFolder = newTest("183924");
		IFolder configFolder = Utils.createFolder(buildFolder, "configuration/org.eclipse.update");

		//Figure out the version of the org.eclipse.rcp feature
		String baseLocation = Platform.getInstallLocation().getURL().getPath();
		File features = new File(baseLocation, "features");
		FileFilter filter = new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().startsWith("org.eclipse.rcp_");
			}
		};
		File rcp[] = features.listFiles(filter);
		assertTrue(rcp.length > 0);
		String name = rcp[0].getName();
		String version = name.substring("org.eclipse.rcp_".length(), name.length());

		// copy platform.xml and set the baseLocation and rcp version
		IFile sourceXML = buildFolder.getFile("platform.xml");
		Map replacements = new HashMap();
		replacements.put("BASE_LOCATION", baseLocation);
		replacements.put("RCP_VERSION", version);
		Utils.transferAndReplace(sourceXML.getLocationURI().toURL(), configFolder.getFile("platform.xml"), replacements);

		//Generate Scripts for org.eclipse.rcp, expect to find it through platform.xml
		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "org.eclipse.rcp");
		properties.put("baseLocation", buildFolder.getLocation().toOSString());
		generateScripts(buildFolder, properties);

		//platform.xml has MANAGED-ONLY policy, expect to not find org.eclipse.core.resources
		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "org.eclipse.core.resources");
		properties.put("baseLocation", buildFolder.getLocation().toOSString());
		try {
			//this is expected to fail
			generateScripts(buildFolder, properties);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e.getMessage().endsWith("Unable to find element: org.eclipse.core.resources."));
		}
	}

	public void _testBug221855() throws Exception {
		IFolder buildFolder = newTest("221855");
		IFolder tempFolder = Utils.createFolder(buildFolder, "temp");
		Utils.generateBundle(tempFolder, "org.eclipse.pde.build.test.221855");

		String configLocation = Platform.getConfigurationLocation().getURL().getFile();
		if (!new File(configLocation, "org.eclipse.equinox.simpleconfigurator/bundles.info").exists())
			fail("bundles.info not Found.");

		File testBundle = null;
		try {
			String baseLocation = Platform.getInstallLocation().getURL().getPath();

			testBundle = new File(baseLocation, "plugins/org.eclipse.pde.build.test.221855");
			new File(testBundle, "META-INF").mkdirs();
			new File(testBundle, "src").mkdir();

			IFile buildProperties = tempFolder.getFile("build.properties");
			IFile manifest = tempFolder.getFile("META-INF/MANIFEST.MF");

			buildProperties.getLocation().toFile().renameTo(new File(testBundle, "build.properties"));
			manifest.getLocation().toFile().renameTo(new File(testBundle, "META-INF/MANIFEST.MF"));

			buildProperties.delete(true, null);
			manifest.delete(true, null);

			Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "org.eclipse.pde.build.test.221855");
			properties.put("filterP2Base", "true");
			try {
				generateScripts(buildFolder, properties);
				fail("Script generation was expected to fail.");
			} catch (Exception e) {
				IFile log = buildFolder.getFile("log.log");
				assertLogContainsLine(log, "Unable to find element: org.eclipse.pde.build.test.221855.");
			}

			properties.put("filterP2Base", "false");
			generateScripts(buildFolder, properties);
		} finally {
			new File(testBundle, "META-INF/MANIFEST.MF").delete();
			new File(testBundle, "META-INF").delete();
			new File(testBundle, "src").delete();
			new File(testBundle, "build.properties").delete();
			new File(testBundle, "build.xml").delete();
			testBundle.delete();
		}
	}

	// test that the order of features passed to FeatureGenerator is preserved
	public void testBug187809() throws Exception {
		IFolder buildFolder = newTest("187809");

		Utils.generateFeature(buildFolder, "sdk", new String[] {"foo", "bar", "disco"}, null);

		assertResourceFile(buildFolder, "features/sdk/feature.xml");
		IFile feature = buildFolder.getFile("features/sdk/feature.xml");
		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature model = factory.parseBuildFeature(feature.getLocationURI().toURL());

		FeatureEntry[] included = model.getIncludedFeatureReferences();
		assertEquals(included.length, 3);
		assertEquals(included[0].getId(), "foo");
		assertEquals(included[1].getId(), "bar");
		assertEquals(included[2].getId(), "disco");
	}

	public void testBug203270() throws Exception {
		//test the highest version of a bundle is selected when both are resolved
		IFolder buildFolder = newTest("203270");

		IFolder A1 = Utils.createFolder(buildFolder, "plugins/a_1");
		IFolder A2 = Utils.createFolder(buildFolder, "plugins/a_3");
		IFolder A3 = Utils.createFolder(buildFolder, "plugins/a_2");

		Utils.generateBundle(A1, "a", "1.0.0.1");
		Utils.generateBundle(A2, "a", "1.0.0.3");
		Utils.generateBundle(A3, "a", "1.0.0.2");

		Utils.generateFeature(buildFolder, "f", null, new String[] {"a;version=1.0.0.qualifier"});

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f");
		properties.put("baseLocation", " ");
		generateScripts(buildFolder, properties);

		assertResourceFile(buildFolder, "plugins/a_3/build.xml");
	}

	// Test that & characters in classpath are escaped properly
	public void testBug125577() throws Exception {
		IFolder buildFolder = newTest("125577");
		Utils.createFolder(buildFolder, "plugins");

		//Create Bundle A
		IFolder bundleA = buildFolder.getFolder("plugins/A & A");
		bundleA.create(true, true, null);
		Utils.generateBundle(bundleA, "A");

		//Create Bundle B
		IFolder bundleB = buildFolder.getFolder("plugins/B");
		bundleB.create(true, true, null);
		Utils.generatePluginBuildProperties(bundleB, null);

		// Bundle B requires Bundle A
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "A");
		Utils.generateBundleManifest(bundleB, "B", "1.0.0", manifestAdditions);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "B"));

		assertResourceFile(bundleB, "build.xml");
		//if the & was not escaped, it won't be a valid ant script
		assertValidAntScript(bundleB.getFile("build.xml"));
	}

	public void testSimpleClasspath() throws Exception {
		IFolder buildFolder = newTest("SimpleClasspath");

		Utils.generatePluginBuildProperties(buildFolder, null);
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), EQUINOX_PREFERENCES);
		Utils.generateBundleManifest(buildFolder, "bundle", "1.0.0", manifestAdditions);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle"));

		IFile buildScript = buildFolder.getFile("build.xml");
		Project antProject = assertValidAntScript(buildScript);
		Target dot = (Target) antProject.getTargets().get("@dot");
		assertNotNull(dot);
		Object child = AntUtils.getFirstChildByName(dot, "path");
		assertNotNull(child);
		assertTrue(child instanceof Path);
		String path = child.toString();

		//Assert classpath has correct contents
		int idx[] = {0, path.indexOf(EQUINOX_PREFERENCES), path.indexOf(OSGI), path.indexOf(EQUINOX_COMMON), path.indexOf(EQUINOX_REGISTRY), path.indexOf(CORE_JOBS)};
		for (int i = 0; i < idx.length - 1; i++) {
			assertTrue(idx[i] < idx[i + 1]);
		}
	}

	public void testBug207500() throws Exception {
		IFolder buildFolder = newTest("207500");

		Utils.generatePluginBuildProperties(buildFolder, null);
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.swt");
		Utils.generateBundleManifest(buildFolder, "bundle", "1.0.0", manifestAdditions);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle"));

		IFile buildScript = buildFolder.getFile("build.xml");
		Project antProject = assertValidAntScript(buildScript);
		Target dot = (Target) antProject.getTargets().get("@dot");
		assertNotNull(dot);
		Object child = AntUtils.getFirstChildByName(dot, "path");
		assertNotNull(child);
		assertTrue(child instanceof Path);
		String path = child.toString();

		//Assert classpath has the swt fragment
		String swtFragment = "org.eclipse.swt." + Platform.getWS() + '.' + Platform.getOS();
		if (!Platform.getWS().equals("carbon") && !Platform.getWS().equals("cocoa"))
			swtFragment += '.' + Platform.getOSArch();
		assertTrue(path.indexOf(swtFragment) > 0);
	}

	public void testPluginPath() throws Exception {
		IFolder buildFolder = newTest("PluginPath");
		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder bundleB = Utils.createFolder(buildFolder, "other/bundleB");

		Utils.generateBundle(bundleA, "bundleA");
		Utils.generateBundle(bundleB, "bundleB");
		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"bundleA", "bundleB"});

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "rcp");
		props.put("pluginPath", bundleB.getLocation().toOSString());

		// generateScripts will assert if bundleB is not found
		generateScripts(buildFolder, props);
	}

	public void testBug128901_filteredDependencyCheck() throws Exception {
		IFolder buildFolder = newTest("128901");
		IFolder bundleFolder = Utils.createFolder(buildFolder, "plugins/bundle");

		Utils.generatePluginBuildProperties(bundleFolder, null);
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), EQUINOX_REGISTRY);
		Utils.generateBundleManifest(bundleFolder, "bundle", "1.0.0", manifestAdditions);

		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"bundle", OSGI, EQUINOX_COMMON, EQUINOX_REGISTRY, CORE_JOBS});

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "rcp");
		props.put("filteredDependencyCheck", "true");

		generateScripts(buildFolder, props);

		// org.eclipse.core.runtime.compatibility.registry is an optional bundle, which should have been excluded 
		//from the state by the filtering, check that is isn't in the classpath 
		IFile buildScript = bundleFolder.getFile("build.xml");
		Project antProject = assertValidAntScript(buildScript);
		Target dot = (Target) antProject.getTargets().get("@dot");
		Object child = AntUtils.getFirstChildByName(dot, "path");
		assertTrue(child instanceof Path);
		String path = child.toString();
		assertTrue(path.indexOf("org.eclipse.core.runtime.compatibility.registry") == -1);
	}

	public void testBug198536() throws Exception {
		final IFolder buildFolder = newTest("198536");

		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature\r\nid=\"foo\" version=\"1.0.0.qualifier\">       \n");
		buffer.append("  <plugin version=\"0.0.0\" id=\"foo\" />              \n");
		buffer.append("  <plugin version=\"1.0.0.id_qualifier\" id=\"bar\" /> \n");
		buffer.append("  <plugin id=\"foo.version\" version=\"0.0.0\"  />     \n");
		buffer.append("</feature>                                             \n");

		IFile featureXML = buildFolder.getFile("feature.xml");
		Utils.writeBuffer(featureXML, buffer);

		buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>          \n");
		buffer.append("<project name=\"project\" default=\"default\">      \n");
		buffer.append("    <target name=\"default\">                       \n");
		buffer.append("    	<eclipse.idReplacer                            \n");
		buffer.append("    			featureFilePath=\"" + featureXML.getLocation().toOSString() + "\"  \n");
		buffer.append("    			selfVersion=\"1.0.0.ABCDE\"            \n");
		buffer.append("    			featureIds=\"\"                        \n");
		buffer.append("    			pluginIds=\"foo:0.0.0,1.0.0.vA,bar:1.0.0,1.0.0.id_v,foo.version:0.0.0,2.1.2\" \n");
		buffer.append("    		/>                                         \n");
		buffer.append("    </target>                                       \n");
		buffer.append("</project>                                          \n");

		final IFile buildXML = buildFolder.getFile("build.xml");
		Utils.writeBuffer(buildXML, buffer);

		runAntScript(buildXML.getLocation().toOSString(), new String[] {"default"}, buildFolder.getLocation().toOSString(), null);

		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature feature = factory.parseBuildFeature(featureXML.getLocationURI().toURL());
		FeatureEntry[] pluginEntryModels = feature.getPluginEntries();
		assertEquals(pluginEntryModels[0].getId(), "foo");
		assertEquals(pluginEntryModels[0].getVersion(), "1.0.0.vA");
		assertEquals(pluginEntryModels[1].getId(), "bar");
		assertEquals(pluginEntryModels[1].getVersion(), "1.0.0.id_v");
		assertEquals(pluginEntryModels[2].getId(), "foo.version");
		assertEquals(pluginEntryModels[2].getVersion(), "2.1.2");
	}

	public void testBug207335() throws Exception {
		IFolder buildFolder = newTest("207335");

		Utils.generatePluginBuildProperties(buildFolder, null);
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.swt");
		Utils.generateBundleManifest(buildFolder, "bundle", "1.0.0", manifestAdditions);

		// give a non-existant directory for the buildDirectory, we are only building a plugin and
		// no features so we shouldn't need it to exist.
		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle");
		properties.put("buildDirectory", buildFolder.getLocation().toOSString() + "/nothing");
		properties.put("pluginPath", buildFolder.getLocation().toOSString());
		generateScripts(buildFolder, properties);
	}

	public void testBug206679() throws Exception {
		IFolder buildFolder = newTest("206679");

		// test that our feature parser throws an exception on an empty requires
		// which would imply that other source generation tests would catch the 
		// bad features

		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"foo\" version=\"1.0.0.qualifier\">   \n");
		buffer.append("  <requires>                                                     \n");
		buffer.append("  </requires>                                                    \n");
		buffer.append("</feature>                                                        \n");

		IFile featureXML = buildFolder.getFile("feature.xml");
		Utils.writeBuffer(featureXML, buffer);

		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		try {
			factory.parseBuildFeature(featureXML.getLocationURI().toURL());
		} catch (CoreException e) {
			assertTrue(e.getStatus().toString().indexOf(Messages.feature_parse_emptyRequires) > 0);
			return;
		}
		assertTrue(false);
	}

	public void testBug193393() throws Exception {
		IFolder buildFolder = newTest("193393");
		IFolder bundleA = Utils.createFolder(buildFolder, "bundleA");

		Utils.generateBundle(bundleA, "bundleA");
		Utils.generateFeature(buildFolder, "featureA", null, new String[] {"bundleA"});

		// move generated feature out into root and get rid of features directory
		File featureA = buildFolder.getFolder("features/featureA").getLocation().toFile();
		featureA.renameTo(new File(buildFolder.getLocation().toFile(), "featureA"));
		buildFolder.getFolder("features").getLocation().toFile().delete();

		// also stick a generic manifest under the feature just to be confusing
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Created-By: 1.4.2 (IBM Corporation)\n");
		buffer.append("Ant-Version: Apache Ant 1.7.0\n");
		IFolder meta = Utils.createFolder(buildFolder, "featureA/META-INF");
		Utils.writeBuffer(meta.getFile("MANIFEST.MF"), buffer);

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "featureA");

		generateScripts(buildFolder, props);
	}

	// also tests that 196754 works without a manifest
	public void testBug196159_196754() throws Exception {
		IFolder buildFolder = newTest("196159");

		Utils.generateFeature(buildFolder, "featureA", null, new String[] {"Plugin21;unpack=\"false\""});

		Utils.storeBuildProperties(buildFolder, BuildConfiguration.getBuilderProperties(buildFolder));
		Utils.generateAllElements(buildFolder, "featureA");

		runBuild(buildFolder);

		IFile javaCompilerArgs = buildFolder.getFile("plugins/Plugin21/javaCompiler.Plugin21.jar.args");
		assertFalse(javaCompilerArgs.exists());
	}

	public void testBug210464() throws Exception {
		IFolder buildFolder = newTest("210464 space");

		Utils.generateFeature(buildFolder, "featureA", null, new String[] {OSGI});

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "featureA"));

		assertResourceFile(buildFolder, "features/featureA/build.xml");
	}

	public void testBug212920() throws Exception {
		IFolder buildFolder = newTest("212920");

		Properties properties = new Properties();
		properties.put("qualifier", "none");
		Utils.generatePluginBuildProperties(buildFolder, properties);
		Utils.generateBundleManifest(buildFolder, "bundle", "1.0.0.qualifier", null);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle"));
	}

	public void testBug224098() throws Exception {
		IFolder buildFolder = newTest("224098");

		Utils.generateFeature(buildFolder, "F", null, null);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<product name=\"Test\" id=\"Test.product\" application=\"org.eclipse.ant.core.antRunner\" useFeatures=\"false\">");
		buffer.append("  <configIni use=\"default\"/>");
		buffer.append("  <launcherArgs>");
		buffer.append("   <vmArgsMac>-XstartOnFirstThread -Dorg.eclipse.swt.internal.carbon.smallFonts</vmArgsMac>");
		buffer.append("  </launcherArgs>");
		buffer.append("</product> ");
		Utils.writeBuffer(buildFolder.getFile("features/F/t.product"), buffer);

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "F");
		props.put("product", "F/t.product");

		generateScripts(buildFolder, props);
	}

	public void testBug208011_simpleCycle() throws Exception {
		IFolder buildFolder = newTest("208011");

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("allowBinaryCycles", "true");
		properties.put("topLevelElementId", "featureA");
		Utils.storeBuildProperties(buildFolder, properties);
		Utils.generateFeature(buildFolder, "featureA", null, new String[] {"B"});

		runBuild(buildFolder);
	}

	public void testBug199241() throws Exception {
		IFolder buildFolder = newTest("199241");
		IFolder fooFolder = Utils.createFolder(buildFolder, "plugins/foo");
		IFolder featureFolder = Utils.createFolder(buildFolder, "features/F");

		Utils.generateBundle(fooFolder, "foo");
		Utils.createFolder(fooFolder, "src");
		Utils.generateFeature(buildFolder, "F", null, new String[] {"foo"});

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "F"));

		assertResourceFile(featureFolder, "build.xml");
		IFile script = featureFolder.getFile("build.xml");
		runAntScript(script.getLocation().toOSString(), new String[] {}, featureFolder.getLocation().toOSString(), null);

		assertResourceFile(featureFolder, "F_1.0.0.jar");
		assertResourceFile(fooFolder, "foo_1.0.0.jar");
	}

	public void testBug237475() throws Exception {
		IFolder buildFolder = newTest("237475");

		Utils.generateFeature(buildFolder, "f", new String[] {"opt;optional=true"}, new String[] {OSGI});
		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f"));
	}

	public void testBug247091() throws Exception {
		IFolder buildFolder = newTest("247091");
		//also tests bug 250942
		Utils.generateFeature(buildFolder, "sdk", new String[] {"f;version=0.0.0", "f;version=1.0.0.qualifier", "f;version=1.0.0.v_qualifier"}, null);
		Utils.generateFeature(buildFolder, "f", null, null, "2.0.0");
		IFolder f = buildFolder.getFolder("features/f");
		f.refreshLocal(IResource.DEPTH_INFINITE, null);
		f.move(new org.eclipse.core.runtime.Path("F1"), true, null);
		Utils.generateFeature(buildFolder, "f", null, null, "1.0.0.z1234");
		f.refreshLocal(IResource.DEPTH_INFINITE, null);
		f.move(new org.eclipse.core.runtime.Path("F2"), true, null);
		Utils.generateFeature(buildFolder, "f", null, null, "1.0.0.v_5678");
		f.refreshLocal(IResource.DEPTH_INFINITE, null);
		f.move(new org.eclipse.core.runtime.Path("F3"), true, null);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk"));

		assertLogContainsLines(buildFolder.getFile("features/sdk/build.xml"), new String[] {"../F1", "../F2", "../F3"});
	}

	public void testBug247091_2() throws Exception {
		VersionRange range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0");
		assertTrue(range.getIncludeMinimum());
		assertTrue(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0"));
		assertEquals(range.getMaximum(), new Version("1.0.0"));

		range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0.qualifier");
		assertTrue(range.getIncludeMinimum());
		assertFalse(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0"));
		assertEquals(range.getMaximum(), new Version("1.0.1"));

		range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0.zqualifier");
		assertTrue(range.getIncludeMinimum());
		assertFalse(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0.z"));
		assertEquals(range.getMaximum(), new Version("1.0.1"));

		range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0.abcqualifier");
		assertTrue(range.getIncludeMinimum());
		assertFalse(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0.abc"));
		assertEquals(range.getMaximum(), new Version("1.0.0.abd"));

		range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0.abzqualifier");
		assertTrue(range.getIncludeMinimum());
		assertFalse(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0.abz"));
		assertEquals(range.getMaximum(), new Version("1.0.0.ac"));

		range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0.abzzqualifier");
		assertTrue(range.getIncludeMinimum());
		assertFalse(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0.abzz"));
		assertEquals(range.getMaximum(), new Version("1.0.0.ac"));

		range = org.eclipse.pde.internal.build.Utils.createVersionRange("1.0.0.abzz_qualifier");
		assertTrue(range.getIncludeMinimum());
		assertFalse(range.getIncludeMaximum());
		assertEquals(range.getMinimum(), new Version("1.0.0.abzz_"));
		assertEquals(range.getMaximum(), new Version("1.0.0.abzza"));

	}

	public void testBug246127() throws Exception {
		IFolder buildFolder = newTest("246127");

		IFolder base = Utils.createFolder(buildFolder, "base");
		IFolder A = Utils.createFolder(base, "dropins/eclipse/plugins/A");
		IFolder B = Utils.createFolder(base, "dropins/plugins/B");
		IFolder C = Utils.createFolder(base, "dropins/random/plugins/C");
		IFolder D = Utils.createFolder(base, "dropins/other/eclipse/plugins/D");
		IFolder E = Utils.createFolder(base, "dropins/E");

		Utils.generateBundle(A, "A");
		Utils.generateBundle(B, "B");
		Utils.generateBundle(C, "C");
		Utils.generateBundle(D, "D");
		Utils.generateBundle(E, "E");

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"A", "B", "C", "D", "E"});

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
		props.put("baseLocation", base.getLocation().toOSString());
		generateScripts(buildFolder, props);

		//success if we found all the bundles and hence did not throw an exception
	}

	public void testBug247232() throws Exception {
		IFolder buildFolder = newTest("247232");

		Utils.generateBundleManifest(buildFolder, "org.foo", "1.2.2.2.3", null);
		Utils.generatePluginBuildProperties(buildFolder, null);
		buildFolder.refreshLocal(IResource.DEPTH_INFINITE, null);

		try {
			generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "org.foo"));
		} catch (Exception e) {
			//ok
		}
		assertLogContainsLines(buildFolder.getFile("log.log"), new String[] {"Problem occurred while considering plugin: Test Bundle org.foo.", "invalid format"});
	}

	public void testBug248767_212467() throws Exception {
		IFolder rootFolder = newTest("248767");
		IFolder build1 = rootFolder.getFolder("build1");
		IFolder build2 = rootFolder.getFolder("build2");

		// Build 1 compiles B against source A
		// A must compile first, testing flatten dependencies (non-flattened order is depth first putting B before A).
		Utils.generateFeature(build1, "F1", new String[] {"F2"}, new String[] {"A;unpack=true", OSGI});
		Utils.generateFeature(build1, "F2", null, new String[] {"B"});

		Properties properties = BuildConfiguration.getBuilderProperties(build1);
		properties.put("topLevelElementId", "F1");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("pluginPath", build2.getLocation().toOSString());
		properties.put("flattenDependencies", "true");
		Utils.storeBuildProperties(build1, properties);
		runBuild(build1);

		build1.refreshLocal(IResource.DEPTH_INFINITE, null);

		assertResourceFile(build1, "compile.F1.xml");
		assertLogContainsLines(build1.getFile("compile.F1.xml"), new String[] {"plugins/A", "plugins/B"});

		build1.getFolder("tmp/eclipse/plugins/B_1.0.0").delete(true, null);

		//Build 2 compiles B against binary A
		Utils.generateFeature(build2, "F2", null, new String[] {"A", "B"});
		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("topLevelElementId", "F2");
		properties.put("baseLocation", build1.getFolder("tmp/eclipse").getLocation().toOSString());
		Utils.storeBuildProperties(build2, properties);
		runBuild(build2);
	}

	public void testBug238177() throws Exception {
		IFolder buildFolder = newTest("238177");
		IFolder a = Utils.createFolder(buildFolder, "plugins/A");
		IFolder b = Utils.createFolder(buildFolder, "plugins/B");
		IFolder c = Utils.createFolder(buildFolder, "plugins/C");
		IFolder d = Utils.createFolder(buildFolder, "plugins/D");
		IFolder e = Utils.createFolder(buildFolder, "plugins/E");
		IFolder f = Utils.createFolder(buildFolder, "plugins/F");

		Utils.generateFeature(buildFolder, "feature", null, new String[] {"A", "B", "C", "D", "E", "F"});

		Utils.generateBundleManifest(a, "A", "1.0.0", null);
		Utils.generateBundle(a, "A");
		Utils.generateBundle(b, "B");

		Attributes attributes = new Attributes();
		Attributes.Name requireAttribute = new Attributes.Name("Require-Bundle");
		attributes.put(requireAttribute, "A");
		Utils.generateBundleManifest(c, "C", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(c, null);

		attributes.put(requireAttribute, "A, B");
		Utils.generateBundleManifest(d, "D", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(d, null);

		attributes.put(requireAttribute, "C, B, D");
		Utils.generateBundleManifest(e, "E", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(e, null);

		attributes.put(requireAttribute, "C, D, E");
		Utils.generateBundleManifest(f, "F", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(f, null);

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "feature");
		properties.put("flattenDependencies", "true");
		properties.put("parallelCompilation", "true");
		generateScripts(buildFolder, properties);

		IFile buildScript = buildFolder.getFile("compile.feature.xml");
		Project antProject = assertValidAntScript(buildScript);
		Target main = (Target) antProject.getTargets().get("main");
		assertNotNull(main);
		Object[] children = AntUtils.getChildrenByName(main, "parallel");
		assertTrue(children.length == 4);

		Task[] tasks = AntUtils.getParallelTasks((Parallel) children[0]);
		assertTrue(tasks.length == 2);
		String dir0 = (String) tasks[0].getRuntimeConfigurableWrapper().getAttributeMap().get("dir");
		String dir1 = (String) tasks[1].getRuntimeConfigurableWrapper().getAttributeMap().get("dir");
		if (dir0.equals("plugins/B"))
			assertEquals("plugins/A", dir1);
		else {
			assertEquals("plugins/A", dir0);
			assertEquals("plugins/B", dir1);
		}

		tasks = AntUtils.getParallelTasks((Parallel) children[1]);
		assertTrue(tasks.length == 2);
		dir0 = (String) tasks[0].getRuntimeConfigurableWrapper().getAttributeMap().get("dir");
		dir1 = (String) tasks[1].getRuntimeConfigurableWrapper().getAttributeMap().get("dir");
		if (dir0.equals("plugins/C"))
			assertEquals("plugins/D", dir1);
		else {
			assertEquals("plugins/D", dir0);
			assertEquals("plugins/C", dir1);
		}

		tasks = AntUtils.getParallelTasks((Parallel) children[2]);
		assertTrue(tasks.length == 1);
		assertEquals("plugins/E", tasks[0].getRuntimeConfigurableWrapper().getAttributeMap().get("dir"));

		tasks = AntUtils.getParallelTasks((Parallel) children[3]);
		assertTrue(tasks.length == 1);
		assertEquals("plugins/F", tasks[0].getRuntimeConfigurableWrapper().getAttributeMap().get("dir"));
	}

	public static class TestQualifierDirector extends BuildDirector {
		public TestQualifierDirector() {
			super();
			setGenerateVersionSuffix(true);
			setWorkingDirectory("foo");
		}

		public String getQualifierSuffix(BuildTimeFeature feature) throws CoreException {
			return super.generateFeatureVersionSuffix(feature);
		}
	}

	public void testQualifierSuffixes() throws Exception {
		BuildTimeFeature f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "1.2.3.aaa-0z-aaa", true));
		f1.setContextQualifierLength(2);

		BuildTimeFeature f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "1.2.3.aaa-10-aaa", true));
		f2.setContextQualifierLength(2);

		TestQualifierDirector director = new TestQualifierDirector();
		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);

		f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "1.2.3.abcd", true));
		f1.setContextQualifierLength(2);

		f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "1.2.4.aaaa", true));
		f2.setContextQualifierLength(2);

		director = new TestQualifierDirector();
		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);

		f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "4.5.6.xyz", true));
		f1.addEntry(new FeatureEntry("b", "1.2.3.abccccc", true));
		f1.setContextQualifierLength(2);

		f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "4.5.6.xyz", true));
		f2.addEntry(new FeatureEntry("b", "1.2.3.abcd", true));
		f2.setContextQualifierLength(2);

		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);

		f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "1.2.3.abcdefg", true));
		f1.addEntry(new FeatureEntry("b", "4.5.6.xyz", true));
		f1.setContextQualifierLength(2);

		f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "1.2.3.abcdefg", true));
		f2.addEntry(new FeatureEntry("b", "4.5.6.xyz-", true));
		f2.setContextQualifierLength(2);

		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);

		f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "1.2.3.abcdefg", true));
		f1.addEntry(new FeatureEntry("b", "4.5.6.xyz", true));
		f1.setContextQualifierLength(2);

		f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "1.2.3.abcdefg", true));
		f2.addEntry(new FeatureEntry("b", "4.5.6.xyz-", true));
		f2.setContextQualifierLength(2);

		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);

		f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "1.2.3.aAb", true));
		f1.setContextQualifierLength(2);

		f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "1.2.3.a_b", true));
		f2.setContextQualifierLength(2);

		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);

		f1 = new BuildTimeFeature("foo", "1.0.0.v1");
		f1.addEntry(new FeatureEntry("a", "1.2.3.aZ", true));
		f1.setContextQualifierLength(2);

		f2 = new BuildTimeFeature("foo", "1.0.0.v1");
		f2.addEntry(new FeatureEntry("a", "1.2.3.a_", true));
		f2.setContextQualifierLength(2);

		assertTrue(director.getQualifierSuffix(f1).compareTo(director.getQualifierSuffix(f2)) < 0);
	}

	public void testBug156043() throws Exception {
		IFolder buildFolder = newTest("156043");
		IFolder p1 = Utils.createFolder(buildFolder, "plugins/p1");
		IFolder p2 = Utils.createFolder(buildFolder, "plugins/p2");
		IFolder f1 = Utils.createFolder(buildFolder, "features/F1");
		Utils.generateFeature(buildFolder, "F1", new String[] {"F2"}, new String[] {"P1"});
		Utils.generateFeature(buildFolder, "F2", null, new String[] {"P2"});

		Utils.generateBundle(p1, "P1");
		Utils.writeBuffer(p1.getFile("src/a.java"), new StringBuffer("class A implements foo {}"));

		Utils.generateBundle(p2, "P2");
		Utils.writeBuffer(p2.getFile("src/b.java"), new StringBuffer("class B implements foo {}"));

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F1");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("javacFailOnError", "false");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("plugins/P1_1.0.0/@dot.log");
		entries.add("plugins/P2_1.0.0/@dot.log");
		assertZipContents(f1, "F1_1.0.0.log.zip", entries);

		//bug 279609
		properties.put("logExtension", ".xml");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		entries.add("plugins/P1_1.0.0/@dot.xml");
		entries.add("plugins/P2_1.0.0/@dot.xml");
		assertZipContents(f1, "F1_1.0.0.log.zip", entries);
	}

	public void testBug239843_1() throws Exception {
		IFolder buildFolder = newTest("239843_1");

		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		Attributes additionalAttributes = new Attributes();
		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Import-Package"), "org.xml.sax");
		Utils.generateBundleManifest(a, "a", "1.0.0", additionalAttributes);

		//1: without any particular profiles defined in build.properties, default to largest (1.6?) which does contain org.xml.sax
		Properties buildProperties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "a");
		buildProperties.put("baseLocation", " ");
		buildProperties.put("pluginPath", FileLocator.getBundleFile(Platform.getBundle(OSGI)).getAbsolutePath());
		generateScripts(buildFolder, buildProperties);

		//2: define CDC-1.1/Foundation-1.1, expect failure since that profile doesn't have org.xml.sax
		buildProperties.put("CDC-1.1/Foundation-1.1", "somejar.jar");
		try {
			generateScripts(buildFolder, buildProperties);
			fail("Script generation expected to fail.");
		} catch (Exception e) {
			assertTrue(e.getMessage().indexOf("Unable to find element: a") > -1);
		}

		//3: add a bundle exporting xml.sax, expect success
		IFolder xml = Utils.createFolder(buildFolder, "plugins/xml");
		additionalAttributes.put(new Attributes.Name("Export-Package"), "org.xml.sax");
		Utils.generateBundleManifest(xml, "org.xml", "1.0.0", additionalAttributes);
		generateScripts(buildFolder, buildProperties);
	}

	public void testBug239843_2() throws Exception {
		IFolder buildFolder = newTest("239843_2");

		//custom profile contributed, without profile.list, in a folder
		IFolder custom = Utils.createFolder(buildFolder, "plugins/custom");
		Utils.generateBundle(custom, "custom");
		StringBuffer buffer = new StringBuffer("osgi.java.profile.name=MyCustomProfile\n");
		buffer.append("org.osgi.framework.system.packages=org.my.package\n");
		buffer.append("org.osgi.framework.bootdelegation = org.my.package\n");
		buffer.append("org.osgi.framework.executionenvironment=MyCustomProfile,OSGi/Minimum-1.2\n");
		Utils.writeBuffer(custom.getFile("my.profile"), buffer);

		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		Attributes additionalAttributes = new Attributes();
		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Import-Package"), "org.my.package");
		Utils.generateBundleManifest(a, "a", "1.0.0", additionalAttributes);

		Properties buildProperties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "a");
		buildProperties.put("baseLocation", " ");
		buildProperties.put("pluginPath", FileLocator.getBundleFile(Platform.getBundle(OSGI)).getAbsolutePath());
		buildProperties.put("customEESources", custom.getLocation().toOSString());
		buildProperties.put("MyCustomProfile", "someLibrary.jar");
		generateScripts(buildFolder, buildProperties);
	}

	public void testBug239843_3() throws Exception {
		IFolder buildFolder = newTest("239843_3");

		//custom profile contributed, with profile.list, in a jar
		IFolder custom = Utils.createFolder(buildFolder, "plugins/custom");
		Utils.generateBundle(custom, "custom");

		StringBuffer buffer = new StringBuffer("osgi.java.profile.name=MyCustomProfile\n");
		buffer.append("org.osgi.framework.system.packages=org.my.package\n");
		buffer.append("org.osgi.framework.bootdelegation = org.my.package\n");
		buffer.append("org.osgi.framework.executionenvironment=MyCustomProfile,OSGi/Minimum-1.2\n");
		Utils.writeBuffer(custom.getFile("profiles/my.profile"), buffer);
		Utils.writeBuffer(custom.getFile("profile.list"), new StringBuffer("java.profiles=profiles/my.profile\n"));

		ZipOutputStream jar = new ZipOutputStream(new FileOutputStream(new File(custom.getLocation().toOSString() + ".jar")));
		jar.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
		Utils.transferStreams(custom.getFile(JarFile.MANIFEST_NAME).getContents(), true, jar, false);
		jar.putNextEntry(new ZipEntry("profile.list"));
		Utils.transferStreams(custom.getFile("profile.list").getContents(), true, jar, false);
		jar.putNextEntry(new ZipEntry("profiles/my.profile"));
		Utils.transferStreams(custom.getFile("profiles/my.profile").getContents(), true, jar, false);
		jar.close();

		custom.delete(true, null);

		IFolder a = Utils.createFolder(buildFolder, "plugins/a");
		Attributes additionalAttributes = new Attributes();
		additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Import-Package"), "org.my.package");
		Utils.generateBundleManifest(a, "a", "1.0.0", additionalAttributes);

		Properties buildProperties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "a");
		buildProperties.put("baseLocation", " ");
		buildProperties.put("pluginPath", FileLocator.getBundleFile(Platform.getBundle(OSGI)).getAbsolutePath());
		buildProperties.put("customEESources", custom.getLocation().toOSString() + ".jar");
		buildProperties.put("MyCustomProfile", "someLibrary.jar");
		generateScripts(buildFolder, buildProperties);
	}

	public void testBug262294() throws Exception {
		IFolder buildFolder = newTest("262294");

		IFolder cdc = Utils.createFolder(buildFolder, "plugins/cdc");
		Attributes additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Bundle-RequiredExecutionEnvironment"), "CDC-1.1/Foundation-1.1");
		Utils.generateBundleManifest(cdc, "cdc", "1.0.0", additionalAttributes);

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new org.eclipse.core.runtime.Path("/scripts/productBuild/productBuild.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();

		Properties generateProperties = new Properties();
		generateProperties.put("buildDirectory", buildFolder.getLocation().toOSString());
		generateProperties.put("baseLocation", " ");
		generateProperties.put("verify", "true");
		generateProperties.put("includeLaunchers", "false");
		generateProperties.put("configs", "*,*,*");
		generateProperties.put("CDC-1.1/Foundation-1.1", "here");
		generateProperties.put("pluginList", "cdc");
		runAntScript(buildXMLPath, new String[] {"generateFeature"}, buildFolder.getLocation().toOSString(), generateProperties);
	}

	public void testRootFiles_1() throws Exception {
		IFolder buildFolder = newTest("RootFiles_1");
		IFolder f = Utils.createFolder(buildFolder, "features/F");
		Utils.generateFeature(buildFolder, "F", null, null);
		Properties properties = new Properties();
		properties.put("root", "file:1, dir");
		properties.put("root.folder.sub", "file:2");
		properties.put("root.win32.win32.x86", "file:3");
		properties.put("root.win32.win32.x86.folder.other", "file:4, other");
		Utils.storeBuildProperties(f, properties);

		Utils.writeBuffer(f.getFile("1"), new StringBuffer("1"));
		Utils.writeBuffer(f.getFile("2"), new StringBuffer("2"));
		Utils.writeBuffer(f.getFile("3"), new StringBuffer("3"));
		Utils.writeBuffer(f.getFile("4"), new StringBuffer("4"));
		Utils.writeBuffer(f.getFile("dir/5"), new StringBuffer("5"));
		Utils.writeBuffer(f.getFile("other/6"), new StringBuffer("6"));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("configs", "*,*,* & win32,win32,x86");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Set zipEntries = new HashSet();
		zipEntries.add("eclipse/1");
		zipEntries.add("eclipse/sub/2");
		zipEntries.add("eclipse/3");
		zipEntries.add("eclipse/other/4");
		zipEntries.add("eclipse/5");
		zipEntries.add("eclipse/other/6");
		assertZipContents(buildFolder, "I.TestBuild/F-TestBuild-win32.win32.x86.zip", zipEntries);

		zipEntries.clear();
		zipEntries.add("eclipse/1");
		zipEntries.add("eclipse/sub/2");
		zipEntries.add("eclipse/5");
		assertZipContents(buildFolder, "I.TestBuild/F-TestBuild.zip", zipEntries);
	}

	public void testBug256787() throws Exception {
		IFolder buildFolder = newTest("256787");

		StringBuffer buffer = new StringBuffer();
		buffer.append("<project name=\"test\" basedir=\".\">   \n");
		buffer.append("   <target name=\"main\">               \n");
		buffer.append("      <eclipse.versionReplacer path=\"");
		buffer.append(buildFolder.getLocation().toOSString());
		buffer.append("\" version=\"3.5.0.v20081125\" />       \n");
		buffer.append("   </target>                            \n");
		buffer.append("</project>                              \n");

		IFile xml = buildFolder.getFile("build.xml");
		Utils.writeBuffer(xml, buffer);

		runAntScript(xml.getLocation().toOSString(), new String[] {"main"}, buildFolder.getLocation().toOSString(), null);
		assertLogContainsLine(buildFolder.getFile("META-INF/MANIFEST.MF"), "Bundle-RequiredExecutionEnvironment: J2SE-1.4");
	}

	public void testBug260634() throws Exception {
		IFolder buildFolder = newTest("260634");

		IFolder bundle = Utils.createFolder(buildFolder, "plugins/bundle");
		Utils.writeBuffer(bundle.getFile("src/A.java"), new StringBuffer("public class A { int i;}"));
		Utils.generateBundleManifest(bundle, "bundle", "1.0.0", null);
		Utils.generatePluginBuildProperties(bundle, null);
		Utils.writeBuffer(bundle.getFile("src/META-INF/MANIFEST.MF"), new StringBuffer("Manifest-Version: 1.0\n"));

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "bundle"));
		String buildXMLPath = bundle.getFile("build.xml").getLocation().toOSString();
		runAntScript(buildXMLPath, new String[] {"build.update.jar"}, buildFolder.getLocation().toOSString(), null);

		assertResourceFile(bundle, "bundle_1.0.0.jar");
		JarFile jar = null;
		try {
			jar = new JarFile(bundle.getFile("bundle_1.0.0.jar").getLocation().toFile());
			Manifest manifest = jar.getManifest();
			assertEquals(manifest.getMainAttributes().getValue("Bundle-SymbolicName"), "bundle");
		} finally {
			jar.close();
		}
	}

	public void testBug217005() throws Exception {
		IFolder buildFolder = newTest("217005");
		IFolder f = Utils.createFolder(buildFolder, "features/f");
		IFolder shape = Utils.createFolder(buildFolder, "plugins/shape");

		Attributes additionalAttributes = new Attributes();
		additionalAttributes.put(new Attributes.Name("Eclipse-BundleShape"), "jar");
		Utils.generateBundleManifest(shape, "shape", "1.0.0", additionalAttributes);

		StringBuffer buffer = new StringBuffer();
		buffer.append("<feature id=\"f\" version=\"1.0.0\">             \n");
		buffer.append("  <plugin version=\"0.0.0\" id=\"shape\" />      \n");
		buffer.append("</feature>                                       \n");

		IFile featureXML = f.getFile("feature.xml");
		Utils.writeBuffer(featureXML, buffer);
		Properties properties = new Properties();
		properties.put("bin.includes", "feature.xml");
		Utils.storeBuildProperties(f, properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("configs", "*,*,*");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		assertResourceFile(buildFolder, "tmp/eclipse/plugins/shape_1.0.0.jar");
	}

	public void testBug219832() throws Exception {
		IFolder buildFolder = newTest("219832");

		IFolder p1 = Utils.createFolder(buildFolder, "plugins/p1");

		Utils.generateFeature(buildFolder, "f", null, new String[] {"p1;unpack=false"});

		Utils.generateBundle(p1, "p1");
		Utils.writeBuffer(p1.getFile("src/a.java"), new StringBuffer("class A {}"));

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new org.eclipse.core.runtime.Path("/resources/keystore/keystore"), null);
		assertNotNull(resource);
		String keystorePath = FileLocator.toFileURL(resource).getPath();

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("configs", "*,*,*");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("signJars", "true");
		properties.put("sign.alias", "pde.build");
		properties.put("sign.keystore", keystorePath);
		properties.put("sign.storepass", "storepass");
		properties.put("sign.keypass", "keypass");

		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IFile result = buildFolder.getFile("tmp/eclipse/plugins/p1_1.0.0.jar");
		assertResourceFile(result);
		assertJarVerifies(result.getLocation().toFile(), true);
	}

	public void testBug190041() throws Exception {
		IFolder buildFolder = newTest("190041");

		IFolder p1 = Utils.createFolder(buildFolder, "plugins/p1");
		IFolder p2 = Utils.createFolder(buildFolder, "plugins/p2");

		Utils.generateFeature(buildFolder, "f", null, new String[] {"p1;unpack=false", "p2;unpack=false"});

		Utils.generateBundle(p1, "p1");
		Utils.writeBuffer(p1.getFile("src/a.java"), new StringBuffer("class A {}"));
		Utils.generateBundle(p2, "p2");
		Utils.writeBuffer(p2.getFile("src/b.java"), new StringBuffer("class B {}"));

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("configs", "*,*,*");
		properties.put("archivePrefix", "");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Set zipEntries = new HashSet();
		zipEntries.add("plugins/p1_1.0.0.jar");
		zipEntries.add("plugins/p2_1.0.0.jar");
		assertZipContents(buildFolder, "I.TestBuild/f-TestBuild.zip", zipEntries);

		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build.tests"), new org.eclipse.core.runtime.Path("/resources/keystore/keystore"), null);
		assertNotNull(resource);
		String keystorePath = FileLocator.toFileURL(resource).getPath();

		File zipFile = buildFolder.getFile("I.TestBuild/f-TestBuild.zip").getLocation().toFile();
		StringBuffer buffer = new StringBuffer();
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>               \n");
		buffer.append("<project name=\"project\" default=\"default\">           \n");
		buffer.append("    <target name=\"default\">                            \n");
		buffer.append("    	<eclipse.jarProcessor                               \n");
		buffer.append("             sign=\"true\"                               \n");
		buffer.append("            	pack=\"true\"                              \n");
		buffer.append("    	        jar=\"" + zipFile.toString() + "\"          \n");
		buffer.append("    			keypass=\"keypass\"                         \n");
		buffer.append("    			storepass=\"storepass\"                     \n");
		buffer.append("    			keystore=\"" + keystorePath + "\"          \n");
		buffer.append("    			alias=\"pde.build\"    />                   \n");
		buffer.append("    </target>                                            \n");
		buffer.append("</project>                                               \n");

		final IFile buildXML = buildFolder.getFile("build.xml");
		Utils.writeBuffer(buildXML, buffer);

		runAntScript(buildXML.getLocation().toOSString(), new String[] {"default"}, buildFolder.getLocation().toOSString(), null);

		zipEntries.add("plugins/p1_1.0.0.jar.pack.gz");
		zipEntries.add("plugins/p2_1.0.0.jar.pack.gz");
		assertZipContents(buildFolder, "I.TestBuild/f-TestBuild.zip", zipEntries);

		java.util.zip.ZipFile zip = null;
		File tempJar = new File(zipFile.getParentFile(), "temp.jar");
		try {
			zip = new java.util.zip.ZipFile(zipFile);
			ZipEntry entry = zip.getEntry("plugins/p1_1.0.0.jar");
			OutputStream output = new BufferedOutputStream(new FileOutputStream(tempJar));
			Utils.transferStreams(zip.getInputStream(entry), true, output, true);
		} finally {
			zip.close();
		}
		assertJarVerifies(tempJar, true);
	}

	public void testBug279583() throws Exception {
		IFolder buildFolder = newTest("279583");

		String bundleId = "org.eclipse.pde.build.test.279583";
		Utils.generateBundleManifest(buildFolder, bundleId, "1.0.0", null);
		Properties extraProperties = new Properties();
		extraProperties.put("jars.extra.classpath", "platform:/plugins/foo/k.jar");
		Utils.generatePluginBuildProperties(buildFolder, extraProperties);

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", bundleId);
		properties.put("baseLocation", buildFolder.getLocation().toOSString());
		try {
			generateScripts(buildFolder, properties);
			fail("We expected an exception");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Malformed URL exception: org.eclipse.pde.build.test.279583/build.properties: platform:/plugins/foo/k.jar.");
		}
	}

	public void testBug281592() throws Exception {
		IFolder buildFolder = newTest("281592");

		IFolder B = Utils.createFolder(buildFolder, "plugins/B");
		Attributes additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(B, null);
		additionalAttributes.put(new Attributes.Name("Export-Package"), "b");
		additionalAttributes.put(new Attributes.Name("Import-Package"), "x;version=\"[3.0.0,4.0.0)\"");
		Utils.generateBundleManifest(B, "B", "1.0.0", additionalAttributes);
		StringBuffer code = new StringBuffer();
		code.append("package b;                  \n");
		code.append("import x.X;                 \n");
		code.append("public class B{             \n");
		code.append("   static public void f() { \n");
		code.append("      X.g();                \n");
		code.append("   }                        \n");
		code.append("}                           \n");
		Utils.writeBuffer(B.getFile("src/b/B.java"), code);

		IFolder D = Utils.createFolder(buildFolder, "plugins/D");
		additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(D, null);
		additionalAttributes.put(new Attributes.Name("Import-Package"), "b, x;version=\"[2.0.0, 3.0.0)\"");
		Utils.generateBundleManifest(D, "D", "1.0.0", additionalAttributes);
		code = new StringBuffer();
		code.append("package d;                \n");
		code.append("import x.X;               \n");
		code.append("import b.B;               \n");
		code.append("public class D{           \n");
		code.append("   public void f() {      \n");
		code.append("      B.f();              \n");
		code.append("      X.f();              \n");
		code.append("   }                      \n");
		code.append("}                         \n");
		Utils.writeBuffer(D.getFile("src/d/D.java"), code);

		IFolder X2 = Utils.createFolder(buildFolder, "plugins/X_2");
		additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(X2, null);
		additionalAttributes.put(new Attributes.Name("Export-Package"), "x;version=\"2.0.1\"");
		Utils.generateBundleManifest(X2, "X", "2.0.0", additionalAttributes);
		Utils.writeBuffer(X2.getFile("src/x/X.java"), new StringBuffer("package x;\n public class X { public static void f(){} }"));

		IFolder X3 = Utils.createFolder(buildFolder, "plugins/X_3");
		additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(X3, null);
		additionalAttributes.put(new Attributes.Name("Export-Package"), "x;version=\"3.0.2\"");
		Utils.generateBundleManifest(X3, "X", "3.0.0", additionalAttributes);
		Utils.writeBuffer(X3.getFile("src/x/X.java"), new StringBuffer("package x;\n public class X { public static void g(){} }"));

		Utils.generateFeature(buildFolder, "f", null, new String[] {"B", "D", "X;version=2.0.0", "X;version=3.0.0"});
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("logExtension", ".xml");
		properties.put("baseLocation", "");
		properties.put("pluginPath", "${buildDirectory}"); //178449
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug279622() throws Exception {
		IFolder buildFolder = newTest("279622");

		IFolder B = Utils.createFolder(buildFolder, "plugins/B");
		Attributes additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(B, null);
		additionalAttributes.put(new Attributes.Name("Export-Package"), "b");
		additionalAttributes.put(new Attributes.Name("Import-Package"), "x;version=\"[3.0.0,4.0.0)\"");
		Utils.generateBundleManifest(B, "B", "1.0.0", additionalAttributes);
		StringBuffer code = new StringBuffer();
		code.append("package b;                  \n");
		code.append("import x.X;                 \n");
		code.append("public class B{             \n");
		code.append("   static public void f() { \n");
		code.append("      X.g();                \n");
		code.append("   }                        \n");
		code.append("}                           \n");
		Utils.writeBuffer(B.getFile("src/b/B.java"), code);

		IFolder D = Utils.createFolder(buildFolder, "plugins/D");
		additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(D, null);
		additionalAttributes.put(new Attributes.Name("Import-Package"), "b, x;version=\"[2.0.0, 3.0.0)\"");
		Utils.generateBundleManifest(D, "D", "1.0.0", additionalAttributes);
		code = new StringBuffer();
		code.append("package d;                \n");
		code.append("import x.X;               \n");
		code.append("import b.B;               \n");
		code.append("public class D{           \n");
		code.append("   public void f() {      \n");
		code.append("      B.f();              \n");
		code.append("      X.f();              \n");
		code.append("   }                      \n");
		code.append("}                         \n");
		Utils.writeBuffer(D.getFile("src/d/D.java"), code);

		IFolder X2 = Utils.createFolder(buildFolder, "plugins/X_2");
		additionalAttributes = new Attributes();
		Utils.generatePluginBuildProperties(X2, null);
		additionalAttributes.put(new Attributes.Name("Export-Package"), "x;version=\"2.0.1\", x;version=\"3.0.2\"");
		Utils.generateBundleManifest(X2, "X", "2.0.0", additionalAttributes);
		Utils.writeBuffer(X2.getFile("src/x/X.java"), new StringBuffer("package x;\n public class X { public static void f(){} public static void g(){} }"));

		Utils.generateFeature(buildFolder, "f", null, new String[] {"B", "D", "X"});
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("logExtension", ".xml");
		properties.put("baseLocation", "");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug284721() throws Exception {
		IFolder buildFolder = newTest("284721");

		IFolder host = Utils.createFolder(buildFolder, "plugins/host");
		IFolder fA = Utils.createFolder(buildFolder, "plugins/fA");
		IFolder fB = Utils.createFolder(buildFolder, "plugins/fB");
		IFolder C = Utils.createFolder(buildFolder, "plugins/C");
		IFolder D = Utils.createFolder(buildFolder, "plugins/D");

		Attributes additional = new Attributes();
		additional.put(new Attributes.Name("Eclipse-ExtensibleAPI"), "true");
		Utils.generateBundleManifest(host, "host", "1.0.0", additional);

		additional = new Attributes();
		additional.put(new Attributes.Name("Eclipse-PlatformFilter"), "(osgi.ws=win32)");
		additional.put(new Attributes.Name("Export-Package"), "api");
		additional.put(new Attributes.Name("Fragment-Host"), "host;bundle-version=\"[1.0.0,1.0.0]\"");
		Utils.generateBundleManifest(fA, "fA", "1.0.0", additional);
		Utils.generatePluginBuildProperties(fA, null);

		StringBuffer code = new StringBuffer();
		code.append("package api;                \n");
		code.append("public class A{             \n");
		code.append("   public static int a = 1; \n");
		code.append("}                           \n");
		Utils.writeBuffer(fA.getFile("src/api/A.java"), code);

		additional = new Attributes();
		additional.put(new Attributes.Name("Eclipse-PlatformFilter"), "(osgi.ws=cocoa)");
		additional.put(new Attributes.Name("Export-Package"), "api");
		additional.put(new Attributes.Name("Fragment-Host"), "host;bundle-version=\"[1.0.0,1.0.0]\"");
		Utils.generateBundleManifest(fB, "fB", "1.0.0", additional);
		Utils.generatePluginBuildProperties(fB, null);

		code = new StringBuffer();
		code.append("package api;                \n");
		code.append("public class A{             \n");
		code.append("   public static int b = 1; \n");
		code.append("}                           \n");
		Utils.writeBuffer(fB.getFile("src/api/A.java"), code);

		//bug 105631
		code = new StringBuffer();
		code.append("package c;                  \n");
		code.append("public class CError{        \n");
		code.append("  not going to compile      \n");
		code.append("}                           \n");
		Utils.writeBuffer(C.getFile("src/c/CD.java"), code);

		Properties extraProperties = new Properties();
		extraProperties.put("exclude..", "**/CD.java");

		additional = new Attributes();
		additional.put(new Attributes.Name("Require-Bundle"), "host");
		additional.put(new Attributes.Name("Eclipse-PlatformFilter"), "(osgi.ws=win32)");
		Utils.generateBundleManifest(C, "C", "1.0.0", additional);
		Utils.generatePluginBuildProperties(C, extraProperties);

		code = new StringBuffer();
		code.append("package c;                  \n");
		code.append("import api.A;               \n");
		code.append("public class C{             \n");
		code.append("   public void f() {        \n");
		code.append("      A.a = 2;              \n");
		code.append("   }                        \n");
		code.append("}                           \n");
		Utils.writeBuffer(C.getFile("src/c/C.java"), code);

		additional = new Attributes();
		additional.put(new Attributes.Name("Require-Bundle"), "host");
		additional.put(new Attributes.Name("Eclipse-PlatformFilter"), "(osgi.ws=cocoa)");
		Utils.generateBundleManifest(D, "D", "1.0.0", additional);
		Utils.generatePluginBuildProperties(D, null);

		code = new StringBuffer();
		code.append("package d;                  \n");
		code.append("import api.A;               \n");
		code.append("public class D{             \n");
		code.append("   public void f() {        \n");
		code.append("      A.b = 2;              \n");
		code.append("   }                        \n");
		code.append("}                           \n");
		Utils.writeBuffer(D.getFile("src/d/D.java"), code);

		Utils.generateFeature(buildFolder, "f", null, new String[] {"host", "fA;ws=win32", "fB;ws=cocoa", "C;ws=win32", "D;ws=cocoa"});
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("configs", "win32,win32,x86 & macosx,cocoa,x86");
		properties.put("logExtension", ".xml");
		properties.put("baseLocation", "");
		properties.put("pluginPath", "${buildDirectory}"); //178449
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug252711() throws Exception {
		IFolder buildFolder = newTest("252711");

		IFolder aBinary = Utils.createFolder(buildFolder, "base/plugins/a_3.4.2.v_833");
		Utils.generateBundleManifest(aBinary, "a; singleton:=true", "3.4.2.v_833", null);

		IFolder aBinary2 = Utils.createFolder(buildFolder, "base/plugins/b_1.0.0");
		Utils.generateBundleManifest(aBinary2, "b; singleton:=true", "1.0.0", null);

		IFolder aSource = Utils.createFolder(buildFolder, "plugins/a");
		Utils.generateBundle(aSource, "a; singleton:=true", "3.4.2.Branch_qualifier");

		Utils.generateFeature(buildFolder, "f", null, new String[] {"a;version=\"3.4.2.Branch_qualifier\""});

		Properties buildProperties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f");
		buildProperties.put("baseLocation", buildFolder.getFolder("base").getLocation().toOSString());

		String failedMessage = null;
		try {
			generateScripts(buildFolder, buildProperties);
		} catch (Throwable e) {
			failedMessage = e.getMessage();
		}
		assertTrue(failedMessage != null && failedMessage.indexOf("Another singleton version selected: a_3.4.2.v_833") > -1);
	}

	public void testCatchAllValue() throws Exception {
		IFolder buildFolder = newTest("catchAll");

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		Attributes additional = new Attributes();
		additional.put(new Attributes.Name("Require-Bundle"), "org.eclipse.swt");
		Utils.generateBundleManifest(A, "A", "1.0.0", additional);
		Utils.generatePluginBuildProperties(A, null);

		StringBuffer code = new StringBuffer();
		code.append("package api;                \n");
		code.append("import org.eclipse.swt.widgets.Display;");
		code.append("public class A{             \n");
		code.append("   public static Display d = new Display(); \n");
		code.append("}                           \n");
		Utils.writeBuffer(A.getFile("src/api/A.java"), code);

		Utils.generateFeature(buildFolder, "f", null, new String[] {"A"});
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug291527() throws Exception {
		IFolder buildFolder = newTest("291527");

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		IFolder B = Utils.createFolder(buildFolder, "plugins/B");
		IFolder C = Utils.createFolder(buildFolder, "plugins/C");

		Attributes additional = new Attributes();
		additional.put(new Attributes.Name("Export-Package"), "api;x-friends:=\"B\"");
		Utils.generateBundleManifest(A, "A", "1.0.0", additional);
		Utils.generatePluginBuildProperties(A, null);

		StringBuffer code = new StringBuffer();
		code.append("package api;                \n");
		code.append("public class A{             \n");
		code.append("   public static int a = 1; \n");
		code.append("}                           \n");
		Utils.writeBuffer(A.getFile("src/api/A.java"), code);

		Properties extraProperties = new Properties();
		extraProperties.put("javacErrors..", "unusedLocal");
		Utils.generateBundleManifest(B, "B", "1.0.0", null);
		Utils.generatePluginBuildProperties(B, extraProperties);

		code = new StringBuffer();
		code.append("package api;                \n");
		code.append("public class A{             \n");
		code.append("   public void f() {        \n");
		code.append("      int b = 2;            \n");
		code.append("   }                        \n");
		code.append("}                           \n");
		Utils.writeBuffer(B.getFile("src/api/A.java"), code);

		extraProperties = new Properties();
		extraProperties.put("javacErrors..", "discouraged");

		additional = new Attributes();
		additional.put(new Attributes.Name("Require-Bundle"), "A");
		Utils.generateBundleManifest(C, "C", "1.0.0", additional);
		Utils.generatePluginBuildProperties(C, extraProperties);

		code = new StringBuffer();
		code.append("package c;                  \n");
		code.append("import api.A;               \n");
		code.append("public class C{             \n");
		code.append("   public void f() {        \n");
		code.append("      A.a = 2;              \n");
		code.append("   }                        \n");
		code.append("}                           \n");
		Utils.writeBuffer(C.getFile("src/c/C.java"), code);

		Utils.generateFeature(buildFolder, "f", null, new String[] {"A", "B", "C"});
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		properties.put("baseLocation", "");
		properties.put("pluginPath", "${buildDirectory}"); //178449
		properties.put("javacFailOnError", "false");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		assertLogContainsLines(buildFolder.getFile("I.TestBuild/compilelogs/plugins/B_1.0.0/@dot.log"), new String[] {"The value of the local variable b is not used", "1 problem (1 error)"});
		assertLogContainsLines(buildFolder.getFile("I.TestBuild/compilelogs/plugins/C_1.0.0/@dot.log"), new String[] {"Discouraged access: The type A", "3 problems (3 errors)"});
	}

	public void testBug243582() throws Exception {
		IFolder buildFolder = newTest("243582");
		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		IFolder B = Utils.createFolder(buildFolder, "plugins/B");
		IFolder C = Utils.createFolder(buildFolder, "plugins/C");

		Utils.generateBundle(A, "A");

		Attributes attributes = new Attributes();
		attributes.put(new Attributes.Name(IPDEBuildConstants.ECLIPSE_SOURCE_REF), "${PDE_SOURCE_REF},foo.bar;type:=mine");
		Utils.writeBuffer(B.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));
		Utils.generateBundleManifest(B, "B", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(B, null);

		attributes = new Attributes();
		attributes.put(new Attributes.Name(IPDEBuildConstants.ECLIPSE_SOURCE_REF), "foo.bar;type:=mine");
		Utils.writeBuffer(C.getFile("src/foo.java"), new StringBuffer("public class foo { int i; }"));
		Utils.generateBundleManifest(C, "C", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(C, null);

		Utils.generateFeature(buildFolder, "F", null, new String[] {"A", "B", "C"});

		Properties sourceRefs = new Properties();
		final String a_source = "\"1.0,:pserver:dev.eclipse.org:/cvsroot/rt,org.eclipse.equinox/p2/bundles/org.eclipse.equinox.p2.publisher,org.eclipse.equinox.p2.publisher\";type:=psf;provider:=\"org.eclipse.team.cvs.core.cvsnature\"";
		sourceRefs.put("A,0.0.0", a_source);
		sourceRefs.put("B,0.0.0", "B's source");
		sourceRefs.put("C,0.0.0", "C's source");
		Utils.storeProperties(buildFolder.getFile(IPDEBuildConstants.DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR), sourceRefs);

		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "F");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		properties.put("generateSourceReferences", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Manifest m = Utils.loadManifest(buildFolder.getFile("tmp/eclipse/plugins/A_1.0.0/META-INF/MANIFEST.MF"));
		assertEquals(m.getMainAttributes().getValue(IPDEBuildConstants.ECLIPSE_SOURCE_REF), a_source);
		m = Utils.loadManifest(buildFolder.getFile("tmp/eclipse/plugins/B_1.0.0/META-INF/MANIFEST.MF"));
		assertEquals(m.getMainAttributes().getValue(IPDEBuildConstants.ECLIPSE_SOURCE_REF), "B's source,foo.bar;type:=\"mine\"");
		m = Utils.loadManifest(buildFolder.getFile("tmp/eclipse/plugins/C_1.0.0/META-INF/MANIFEST.MF"));
		assertEquals(m.getMainAttributes().getValue(IPDEBuildConstants.ECLIPSE_SOURCE_REF), "foo.bar;type:=mine");
	}

	public void testBug284806() throws Exception {
		IFolder buildFolder = newTest("284806");
		IFolder A = Utils.createFolder(buildFolder, "plugins/A");

		Attributes attributes = new Attributes();
		attributes.put(new Attributes.Name("Bundle-NativeCode"), "lib.so;selection-filter=\"(osgi.os=foobar)\"");
		Utils.generateBundleManifest(A, "foo", "1.0.0", attributes);
		Utils.writeBuffer(A.getFile("lib.so"), new StringBuffer("I'm a library!"));
		Properties properties = new Properties();
		properties.put("bin.includes", "lib.so, META-INF/, .");
		Utils.generatePluginBuildProperties(A, properties);

		Utils.generateFeature(buildFolder, "f", null, new String[] {"foo"});
		Properties buildProperties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f");
		buildProperties.put("baseLocation", " ");
		buildProperties.put("configs", "win32,win32,x86");

		String failedMessage = null;
		try {
			generateScripts(buildFolder, buildProperties);
		} catch (Throwable e) {
			failedMessage = e.getMessage();
		}
		assertTrue(failedMessage != null && failedMessage.indexOf("Unsatisfied native code filter: lib.so; selection-filter=\"(osgi.os=foobar)\"") > -1);

		properties = new Properties();
		properties.put("buildDirectory", buildFolder.getLocation().toOSString());
		properties.put("baseLocation", " ");
		properties.put("pluginList", "foo");
		properties.put("configs", "win32,win32,x86");
		properties.put("verify", "true");
		URL resource = FileLocator.find(Platform.getBundle("org.eclipse.pde.build"), new org.eclipse.core.runtime.Path("/scripts/productBuild/productBuild.xml"), null);
		String buildXMLPath = FileLocator.toFileURL(resource).getPath();
		runAntScript(buildXMLPath, new String[] {"generateFeature"}, buildFolder.getLocation().toOSString(), properties);
	}

	public void testBug301311() throws Exception {
		Properties antProperties = new Properties();
		antProperties.put(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER, "true");

		BuildScriptGenerator generator = new BuildScriptGenerator() {
			public void setImmutableAntProperties(Properties properties) {
				AbstractScriptGenerator.setStaticAntProperties(properties);
			}
		};
		generator.setImmutableAntProperties(antProperties);

		try {
			Properties newVersions = new Properties();
			newVersions.put("foo,0.0.0", "wildcard");
			newVersions.put("foo,1.0.0", "one");
			newVersions.put("foo,2.0.0.R1_", "r1");

			assertEquals("1.0.0.one", QualifierReplacer.replaceQualifierInVersion("1.0.0.qualifier", "foo", null, newVersions));
			assertEquals("2.0.0.wildcard", QualifierReplacer.replaceQualifierInVersion("2.0.0.qualifier", "foo", null, newVersions));
			assertEquals("2.0.0.R1_r1", QualifierReplacer.replaceQualifierInVersion("2.0.0.R1_qualifier", "foo", null, newVersions));
		} finally {
			generator.setImmutableAntProperties(null);
		}
	}

	// Tests sourceFileExtensions attribute in build.properties
	public void test303960sourceFileExtensions1() throws Exception {
		IFolder buildFolder = newTest("303960_sourceFileExtensions1");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Utils.writeBuffer(plugin.getFile("src/Foo.java"), new StringBuffer("public class Foo { int i; }"));
		Utils.writeBuffer(plugin.getFile("src/Bar.aj"), new StringBuffer("public aspect Bar { int i; }"));

		Properties props = new Properties();
		props.put("sourceFileExtensions", "*.java,   *.aj, *.groovy");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildScript = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildScript);

		// check the excludes directive for copying files over
		Target dot = (Target) antProject.getTargets().get("@dot");
		Copy copyChild = (Copy) AntUtils.getFirstChildByName(dot, "copy");
		Enumeration rc = (Enumeration) ((RuntimeConfigurable) copyChild.getRuntimeConfigurableWrapper().getChildren().nextElement()).getChildren();
		RuntimeConfigurable configurable = (RuntimeConfigurable) rc.nextElement();
		assertEquals(configurable.getElementTag(), "exclude");
		assertEquals(configurable.getAttributeMap().get("name"), "**/*.java");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.aj");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.groovy");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/package.htm*");
		assertFalse("Should have only found 4 filter elements", rc.hasMoreElements());

		// check the includes directive for copying source files
		Target copySource = (Target) antProject.getTargets().get("copy.src.zip");
		copyChild = (Copy) AntUtils.getFirstChildByName(copySource, "copy");
		rc = (Enumeration) ((RuntimeConfigurable) copyChild.getRuntimeConfigurableWrapper().getChildren().nextElement()).getChildren();
		configurable = (RuntimeConfigurable) rc.nextElement();
		assertEquals(configurable.getElementTag(), "include");
		assertEquals(configurable.getAttributeMap().get("name"), "**/*.java");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.aj");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.groovy");
		assertFalse("Should have only found 3 filter elements", rc.hasMoreElements());

		// check the includes directive for zipping source files
		Target zipSource = (Target) antProject.getTargets().get("zip.src.zip");
		Zip zipChild = (Zip) AntUtils.getFirstChildByName(zipSource, "zip");
		rc = (Enumeration) ((RuntimeConfigurable) zipChild.getRuntimeConfigurableWrapper().getChildren().nextElement()).getChildren();
		configurable = (RuntimeConfigurable) rc.nextElement();
		assertEquals(configurable.getElementTag(), "include");
		assertEquals(configurable.getAttributeMap().get("name"), "**/*.java");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.aj");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.groovy");
		assertFalse("Should have only found 3 filter elements", rc.hasMoreElements());
	}

	// Tests sourceFileExtensions attribute in build.properties
	public void test303960sourceFileExtensions2() throws Exception {
		IFolder buildFolder = newTest("303960_sourceFileExtensions2");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Utils.writeBuffer(plugin.getFile("src/Foo.java"), new StringBuffer("public class Foo { int i; }"));
		Utils.writeBuffer(plugin.getFile("src/Bar.aj"), new StringBuffer("public aspect Bar { int i; }"));

		Properties props = new Properties();
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildScript = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildScript);

		// check the excludes directive for copying files over
		Target dot = (Target) antProject.getTargets().get("@dot");
		Copy copyChild = (Copy) AntUtils.getFirstChildByName(dot, "copy");
		Enumeration rc = (Enumeration) ((RuntimeConfigurable) copyChild.getRuntimeConfigurableWrapper().getChildren().nextElement()).getChildren();
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.java");
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/package.htm*");
		assertFalse("Should have only found 2 filter elements", rc.hasMoreElements());

		// check the includes directive for copying source files
		Target copySource = (Target) antProject.getTargets().get("copy.src.zip");
		copyChild = (Copy) AntUtils.getFirstChildByName(copySource, "copy");
		rc = (Enumeration) ((RuntimeConfigurable) copyChild.getRuntimeConfigurableWrapper().getChildren().nextElement()).getChildren();
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.java");
		assertFalse("Should have only found 1 filter elements", rc.hasMoreElements());

		// check the includes directive for zipping source files
		Target zipSource = (Target) antProject.getTargets().get("zip.src.zip");
		Zip zipChild = (Zip) AntUtils.getFirstChildByName(zipSource, "zip");
		rc = (Enumeration) ((RuntimeConfigurable) zipChild.getRuntimeConfigurableWrapper().getChildren().nextElement()).getChildren();
		assertEquals(((RuntimeConfigurable) rc.nextElement()).getAttributeMap().get("name"), "**/*.java");
		assertFalse("Should have only found 1 filter elements", rc.hasMoreElements());
	}

	// Tests compilerAdapter attribute in build.properties
	public void test303960compilerAdapter() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapter");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");
		Object compiler = javac.getRuntimeConfigurableWrapper().getAttributeMap().get("compiler");
		assertEquals("Incorrect compiler adapter", "org.foo.someCompilerAdapter", compiler);
	}

	// Tests compilerAdapter.useLog attribute in build.properties
	public void test303960compilerAdapterUseLog1() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapterUseLog1");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("line") && "-log '${build.result.folder}/@dot${logExtension}'".equals(rc.getAttributeMap().get("line")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					return;
				}
			}
		}
		fail("Should have found compiler log entry:\n-log '${build.result.folder}/@dot${logExtension}'");
	}

	// Tests compilerAdapter.useLog attribute in build.properties
	public void test303960compilerAdapterUseLog2() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapterUseLog2");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		props.put("compilerAdapter.useLog", "true");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("line") && "-log '${build.result.folder}/@dot${logExtension}'".equals(rc.getAttributeMap().get("line")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					return;
				}
			}
		}
		fail("Should have found compiler log entry:\n-log '${build.result.folder}/@dot${logExtension}'");
	}

	// Tests compilerAdapter.useLog attribute in build.properties
	public void test303960compilerAdapterUseLog3() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapterUseLog3");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		props.put("compilerAdapter.useLog", "false");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("line") && "-log '${build.result.folder}/@dot${logExtension}'".equals(rc.getAttributeMap().get("line")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					fail("Should not have found compiler log entry:\n-log '${build.result.folder}/@dot${logExtension}'");
				}
			}
		}
	}

	// Tests compilerAdapter.useArgFile attribute in build.properties
	public void test303960compilerAdapterUseArgFile1() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapterUseArgFile1");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		props.put("javacErrors..", "error"); // force writing an args file
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("value") && "@${basedir}/javaCompiler...args".equals(rc.getAttributeMap().get("value")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					return;
				}
			}
		}
		fail("Should have found compiler log entry:\n@${basedir}/javaCompiler...args");
	}

	// Tests compilerAdapter.useArgFile attribute in build.properties
	public void test303960compilerAdapterUseArgFile2() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapterUseArgFile2");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		props.put("compilerAdapter.useArgFile", "true");
		props.put("javacErrors..", "error"); // force writing an args file
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("value") && "@${basedir}/javaCompiler...args".equals(rc.getAttributeMap().get("value")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					return;
				}
			}
		}
		fail("Should have found compiler log entry:\n@${basedir}/javaCompiler...args");
	}

	// Tests compilerAdapter.useArgFile attribute in build.properties
	public void test303960compilerAdapterUseArgFile3() throws Exception {
		IFolder buildFolder = newTest("303960_compilerAdapterUseArgFile3");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		props.put("compilerAdapter.useArgFile", "false");
		props.put("javacErrors..", "error"); // force writing an args file
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);

		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("value") && "@${basedir}/javaCompiler...args".equals(rc.getAttributeMap().get("value")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					fail("Should not have found compiler log entry:\n@${basedir}/javaCompiler...args");
				}
			}
		}
	}

	// Tests compilerArgs attribute in build.properties
	public void test303960compilerArgs1() throws Exception {
		IFolder buildFolder = newTest("303960_compilerArgs1");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerArg", "-foo -bar baz");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);
		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("line") && "-foo -bar baz".equals(rc.getAttributeMap().get("line"))) {
					return;
				}
			}
		}
		fail("Should have found compiler log entry:\n-foo -bar baz");
	}

	// Tests compilerArgs attribute in build.properties
	public void test303960compilerArgs2() throws Exception {
		IFolder buildFolder = newTest("303960_compilerArgs2");
		Utils.createFolder(buildFolder, "plugins");

		//Create Plugin
		IFolder plugin = buildFolder.getFolder("plugins/Plugin");
		plugin.create(true, true, null);
		Utils.generateBundle(plugin, "Plugin");
		Properties props = new Properties();
		props.put("compilerAdapter", "org.foo.someCompilerAdapter");
		props.put("compilerArg", "-foo -bar baz");
		Utils.generatePluginBuildProperties(plugin, props);

		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "plugin", "Plugin"));

		assertResourceFile(plugin, "build.xml");
		IFile buildxml = plugin.getFile("build.xml");
		Project antProject = assertValidAntScript(buildxml);
		Target dot = (Target) antProject.getTargets().get("@dot");
		Javac javac = (Javac) AntUtils.getFirstChildByName(dot, "javac");

		// check that the build file contains the expected lines
		Enumeration en = javac.getRuntimeConfigurableWrapper().getChildren();
		while (en.hasMoreElements()) {
			RuntimeConfigurable rc = (RuntimeConfigurable) en.nextElement();
			if ("compilerarg".equals(rc.getElementTag())) {
				if (rc.getAttributeMap().containsKey("line") && "-foo -bar baz".equals(rc.getAttributeMap().get("line")) && "org.foo.someCompilerAdapter".equals(rc.getAttributeMap().get("compiler"))) {
					return;
				}
			}
		}
		fail("Should have found compiler log entry:\n-foo -bar baz");
	}

	public void testBug309572() throws Exception {
		IFolder buildFolder = newTest("309572");

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		StringBuffer code = new StringBuffer();
		code.append("package api;                					 \n");
		code.append("import my.view.Activator;  					 \n");
		code.append("public class A{            					 \n");
		code.append("   public static Activator a = new Activator(); \n");
		code.append("}                    					         \n");
		Utils.writeBuffer(A.getFile("src/api/A.java"), code);

		Attributes attributes = new Attributes();
		attributes.put(new Attributes.Name("Require-Bundle"), "test.pluginmodelinstalllocation");
		Utils.generateBundleManifest(A, "A", "1.0.0", attributes);
		Utils.generatePluginBuildProperties(A, null);

		Utils.generateFeature(buildFolder, "f", null, new String[] {"A", "test.pluginmodelinstalllocation"});
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}

	public void testBug265771() throws Exception {
		IFolder buildFolder = newTest("265771");

		IFolder A = Utils.createFolder(buildFolder, "plugins/A");
		Utils.generateBundle(A, "A", "0.0.0");
		StringBuffer manifest = new StringBuffer();
		manifest.append("Bundle-ManifestVersion: 2\n");
		manifest.append("Bundle-Name: Test Bundle\n");
		manifest.append("Bundle-SymbolicName: A\n");
		Utils.writeBuffer(A.getFile("META-INF/MANIFEST.MF"), manifest);

		Utils.generateFeature(buildFolder, "f", null, new String[] {"A"});
		Utils.writeBuffer(buildFolder.getFile("features/f/build.properties"), new StringBuffer("bin.includes=feature.xml\n"));
		Properties properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "f");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		properties.put("p2.gathering", "true");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);
	}
}
