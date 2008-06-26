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
import java.io.FileFilter;
import java.util.*;
import java.util.jar.Attributes;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Path;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.site.*;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;

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

	public void testBug221855() throws Exception {
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
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.equinox.preferences");
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
		int idx[] = {0, path.indexOf("org.eclipse.equinox.preferences"), path.indexOf("org.eclipse.osgi"), path.indexOf("org.eclipse.equinox.common"), path.indexOf("org.eclipse.equinox.registry"), path.indexOf("org.eclipse.core.jobs")};
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
		if (!Platform.getWS().equals("carbon"))
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
		manifestAdditions.put(new Attributes.Name("Require-Bundle"), "org.eclipse.equinox.registry");
		Utils.generateBundleManifest(bundleFolder, "bundle", "1.0.0", manifestAdditions);

		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"bundle", "org.eclipse.osgi", "org.eclipse.equinox.common", "org.eclipse.equinox.registry", "org.eclipse.core.jobs"});

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
		buffer.append("<feature id=\"foo\" version=\"1.0.0.qualifier\">       \n");
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

		Utils.generateFeature(buildFolder, "featureA", null, new String[] {"org.eclipse.osgi"});

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
		
		Utils.generateFeature(buildFolder, "f", new String[] {"opt;optional=true"}, new String[] { "org.eclipse.osgi"} );
		generateScripts(buildFolder, BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "f"));
	}
}
