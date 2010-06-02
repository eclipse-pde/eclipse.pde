/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Ant;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.build.internal.tests.ant.AntUtils;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.eclipse.pde.internal.build.site.BuildTimeFeatureFactory;
import org.eclipse.pde.internal.build.site.compatibility.FeatureEntry;
import org.osgi.framework.FrameworkUtil;

public class SourceTests extends PDETestCase {

	public static Test suite() {
		return new TestSuite(SourceTests.class);
	}

	public void testBug206679_247198() throws Exception {
		IFolder buildFolder = newTest("206679");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		//generate an SDK feature
		Utils.generateFeature(buildFolder, "sdk", new String[] {"org.eclipse.jdt", "jdt.source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@jdt.source", "org.eclipse.jdt");
		Utils.storeBuildProperties(sdk, properties);

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
		//tests bug 247198
		props.put("filteredDependencyCheck", "true");
		generateScripts(buildFolder, props);

		IFolder jdtSource = buildFolder.getFolder("features").getFolder("jdt.source");
		IFile featureXML = jdtSource.getFile("feature.xml");
		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature feature = factory.parseBuildFeature(featureXML.getLocationURI().toURL());
		assertTrue(feature.getDescription() != null);
	}

	public void testBug114150() throws Exception {
		IFolder buildFolder = newTest("114150");

		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		Utils.storeBuildProperties(buildFolder, buildProperties);

		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/features/a.feature.sdk_1.0.0/feature.xml");
		entries.add("eclipse/features/a.feature.source_1.0.0/feature.xml");
		entries.add("eclipse/plugins/a.feature.source_1.0.0/src/a.plugin_1.0.0/src.zip");
		entries.add("eclipse/plugins/a.feature.source_1.0.0/src/a.plugin_1.0.0/about.html"); //tests bug 209092
		assertZipContents(buildFolder, "I.TestBuild/a.feature.sdk.zip", entries);

		entries.add("eclipse/features/a.feature_1.0.0/feature.xml");
		entries.add("eclipse/plugins/a.plugin_1.0.0.jar");
		assertZipContents(buildFolder, "I.TestBuild/a.feature.zip", entries);
	}

	// test that generated source fragments have a proper platform filter
	public void testBug184517() throws Exception {
		IFolder buildFolder = newTest("184517");
		IFolder features = Utils.createFolder(buildFolder, "features");

		//generate an SDK feature
		Utils.generateFeature(buildFolder, "sdk", new String[] {"rcp", "rcp.source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@rcp.source", "rcp");
		IFolder sdk = features.getFolder("sdk");
		Utils.storeBuildProperties(sdk, properties);

		String os = Platform.getOS();
		String ws = Platform.getWS();
		String arch = Platform.getOSArch();

		//Create the rcp feature
		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"fragment;os=\"" + os + "\";ws=\"" + ws + "\";arch=\"" + arch + "\""});

		//Create a fragment with a platform filter
		IFolder fragment = Utils.createFolder(buildFolder, "plugins/fragment");
		Utils.generatePluginBuildProperties(fragment, null);
		Attributes manifestAdditions = new Attributes();
		manifestAdditions.put(new Attributes.Name("Eclipse-PlatformFilter"), "(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))");
		Utils.generateBundleManifest(fragment, "fragment", "1.0.0", manifestAdditions);

		//getScriptGenerationProperties sets buildDirectory to buildFolder by default
		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
		properties.put("configs", os + "," + ws + "," + arch);
		generateScripts(buildFolder, properties);

		String fragmentName = "rcp.source." + os + "." + ws + "." + arch + "_1.0.0";
		IFolder sourceFragment = buildFolder.getFolder("plugins/" + fragmentName);

		// check the manifest for a correct platform filter
		assertResourceFile(sourceFragment, "META-INF/MANIFEST.MF");
		InputStream stream = new BufferedInputStream(sourceFragment.getFile("META-INF/MANIFEST.MF").getLocationURI().toURL().openStream());
		Manifest manifest = new Manifest(stream);
		stream.close();

		String filter = manifest.getMainAttributes().getValue("Eclipse-PlatformFilter");
		assertTrue(filter.length() > 0);
		properties = new Properties();
		properties.put("osgi.os", os);
		properties.put("osgi.ws", ws);
		properties.put("osgi.arch", arch);
		assertTrue(FrameworkUtil.createFilter(filter).match(properties));
	}

	// test that '<' and '>' are properly escaped in generated source feature
	// Also tests bug 191756: features with empty <license> entries
	public void testbug184920() throws Exception {
		//the provided resource features/a.feature/feature.xml contains &lt;foo!&gt; 
		//which must be handled properly
		IFolder buildFolder = newTest("184920");

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "a.feature.sdk");
		//191756: This will NPE if empty license entry is a problem 
		generateScripts(buildFolder, properties);

		assertResourceFile(buildFolder, "features/a.feature.source/feature.xml");
		IFile feature = buildFolder.getFile("features/a.feature.source/feature.xml");

		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		factory.parseBuildFeature(feature.getLocationURI().toURL());
	}

	// test that source can come before the feature it is based on
	public void testBug179616A() throws Exception {
		IFolder buildFolder = newTest("179616A");
		IFolder bundleFolder = Utils.createFolder(buildFolder, "plugins/a.bundle");
		IFolder sdkFolder = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateBundle(bundleFolder, "a.bundle");
		//add some source to a.bundle
		File src = new File(bundleFolder.getLocation().toFile(), "src/a.java");
		src.getParentFile().mkdir();
		FileOutputStream stream = new FileOutputStream(src);
		stream.write("//L33T CODEZ\n".getBytes());
		stream.close();

		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"a.bundle"});

		Utils.generateFeature(buildFolder, "sdk", new String[] {"rcp.source", "rcp"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@rcp.source", "rcp");
		Utils.storeBuildProperties(sdkFolder, properties);

		Utils.generateAllElements(buildFolder, "sdk");
		Utils.storeBuildProperties(buildFolder, BuildConfiguration.getBuilderProperties(buildFolder));
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/plugins/rcp.source_1.0.0/src/a.bundle_1.0.0/src.zip");
		assertZipContents(buildFolder, "I.TestBuild/eclipse.zip", entries);
	}

	public void testBug179616B() throws Exception {
		IFolder buildFolder = newTest("179616B");
		IFolder bundleFolder = Utils.createFolder(buildFolder, "plugins/a.bundle");
		IFolder singleFolder = Utils.createFolder(buildFolder, "features/single");

		Utils.generateBundle(bundleFolder, "a.bundle");
		File src = new File(bundleFolder.getLocation().toFile(), "src/a.java");
		src.getParentFile().mkdir();
		FileOutputStream stream = new FileOutputStream(src);
		stream.write("//L33T CODEZ\n".getBytes());
		stream.close();

		Utils.generateFeature(buildFolder, "single", null, new String[] {"single.source", "a.bundle"});
		Properties properties = new Properties();
		properties.put("generate.plugin@single.source", "single");
		Utils.storeBuildProperties(singleFolder, properties);

		Utils.generateAllElements(buildFolder, "single");
		Utils.storeBuildProperties(buildFolder, BuildConfiguration.getBuilderProperties(buildFolder));
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/plugins/single.source_1.0.0/src/a.bundle_1.0.0/src.zip");
		assertZipContents(buildFolder, "I.TestBuild/eclipse.zip", entries);
	}

	// Test the use of plugin@foo;unpack="false" in the generate.feature property 
	// Test Source generation when source feature is name different from originating feature
	public void testBug107372_208617() throws Exception {
		IFolder buildFolder = newTest("107372");
		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder bundleDoc = Utils.createFolder(buildFolder, "plugins/bundleDoc");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateBundle(bundleA, "bundleA");
		File src = new File(bundleA.getLocation().toFile(), "src/a.java");
		src.getParentFile().mkdir();
		FileOutputStream outputStream = new FileOutputStream(src);
		outputStream.write("//L33T CODEZ\n".getBytes());
		outputStream.close();

		Utils.generateBundle(bundleDoc, "bundleDoc");
		src = new File(bundleDoc.getLocation().toFile(), "src/a.java");
		src.getParentFile().mkdir();
		outputStream = new FileOutputStream(src);
		outputStream.write("//L33T CODEZ\n".getBytes());
		outputStream.close();

		//generate an SDK feature
		//test bug 208617 by naming the source feature something other than just originating feature + .source
		Utils.generateFeature(buildFolder, "sdk", new String[] {"rcp", "source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@source", "rcp,plugin@bundleDoc;unpack=\"false\"");
		Utils.storeBuildProperties(sdk, properties);

		//RCP Feature
		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"bundleA"});

		Utils.generateAllElements(buildFolder, "sdk");
		Utils.storeBuildProperties(buildFolder, BuildConfiguration.getBuilderProperties(buildFolder));
		runBuild(buildFolder);

		//bundleDoc only gets in the build by being added to the generated source feature,
		//check that it is there in the result and is in jar form.
		Set entries = new HashSet();
		entries.add("eclipse/plugins/bundleDoc_1.0.0.jar");
		entries.add("eclipse/plugins/source_1.0.0/src/bundleA_1.0.0/src.zip");
		assertZipContents(buildFolder, "I.TestBuild/eclipse.zip", entries);
	}

	// test use of feature@foo;optional="true"
	public void testBug228537() throws Exception {
		IFolder buildFolder = newTest("228537");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "sdk", new String[] {"source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@source", "sdk,feature@org.eclipse.rcp;optional=\"true\";os=\"win32\"");
		Utils.storeBuildProperties(sdk, properties);

		Properties buildProperties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
		generateScripts(buildFolder, buildProperties);

		assertResourceFile(buildFolder, "features/source/feature.xml");
		IFile featureFile = buildFolder.getFile("features/source/feature.xml");

		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature feature = factory.parseBuildFeature(featureFile.getLocationURI().toURL());
		FeatureEntry[] entries = feature.getRawIncludedFeatureReferences();
		assertTrue(entries.length == 1);
		assertEquals(entries[0].getId(), "org.eclipse.rcp");
		assertTrue(entries[0].isOptional());
		assertEquals(entries[0].getOS(), "win32");
	}

	public void testIndividualSourceBundles() throws Exception {
		IFolder buildFolder = newTest("individualSourceBundles");

		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder bundleB = Utils.createFolder(buildFolder, "plugins/bundleB");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateBundleManifest(bundleA, "bundleA", "1.0.0", null);
		Properties buildProperties = new Properties();
		buildProperties.put("src.includes", "about.html");
		Utils.generatePluginBuildProperties(bundleA, buildProperties);
		File src = new File(bundleA.getLocation().toFile(), "src/a.java");
		src.getParentFile().mkdir();
		FileOutputStream outputStream = new FileOutputStream(src);
		outputStream.write("//L33T CODEZ\n".getBytes());
		outputStream.close();
		File about = new File(bundleA.getLocation().toFile(), "about.html");
		outputStream = new FileOutputStream(about);
		outputStream.write("about\n".getBytes());
		outputStream.close();

		Utils.generateBundle(bundleB, "bundleB");
		src = new File(bundleB.getLocation().toFile(), "src/b.java");
		src.getParentFile().mkdir();
		outputStream = new FileOutputStream(src);
		outputStream.write("//L33T CODEZ\n".getBytes());
		outputStream.close();

		//generate an SDK feature
		Utils.generateFeature(buildFolder, "sdk", new String[] {"rcp", "rcp.source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@rcp.source", "rcp");
		Utils.storeBuildProperties(sdk, properties);

		//RCP Feature
		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"bundleA", "bundleB"});

		Utils.generateAllElements(buildFolder, "sdk");
		buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.add("eclipse/plugins/bundleA.source_1.0.0.jar");
		entries.add("eclipse/plugins/bundleB.source_1.0.0.jar");
		assertZipContents(buildFolder, "I.TestBuild/eclipse.zip", entries);

		ZipFile zip = new ZipFile(buildFolder.getFile("I.TestBuild/eclipse.zip").getLocation().toFile());
		ZipEntry entry = zip.getEntry("eclipse/plugins/bundleA.source_1.0.0.jar");
		InputStream in = new BufferedInputStream(zip.getInputStream(entry));
		IFile jar = buildFolder.getFile("bundleA.source_1.0.0.jar");
		OutputStream out = new BufferedOutputStream(new FileOutputStream(jar.getLocation().toFile()));
		org.eclipse.pde.internal.build.Utils.transferStreams(in, out);
		zip.close();

		entries.clear();
		entries.add("about.html");
		entries.add("plugin.properties");
		assertZipContents(buildFolder, "bundleA.source_1.0.0.jar", entries);

		IFile feature = buildFolder.getFile("features/rcp.source/feature.xml");
		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature model = factory.parseBuildFeature(feature.getLocationURI().toURL());

		FeatureEntry[] included = model.getPluginEntries();
		assertEquals(included.length, 2);
		assertEquals(included[0].getId(), "bundleA.source");
		assertFalse(included[0].isUnpack());
		assertEquals(included[1].getId(), "bundleB.source");
		assertFalse(included[1].isUnpack());
	}

	public void testBug230870() throws Exception {
		IFolder buildFolder = newTest("230870");

		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder bundleB = Utils.createFolder(buildFolder, "plugins/bundleB");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "sdk", new String[] {"rcp", "rcp.source"}, new String[] {"bundleB", "bundleB.source;unpack=false"});
		Properties properties = new Properties();
		properties.put("generate.feature@rcp.source", "rcp");
		properties.put("generate.plugin@bundleB.source", "bundleB");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(sdk, properties);

		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"bundleA"});

		Utils.generateBundleManifest(bundleA, "bundleA", "1.0.0", null);
		Properties buildProperties = new Properties();
		buildProperties.put("src.includes", "about.html");
		Utils.generatePluginBuildProperties(bundleA, buildProperties);
		Utils.writeBuffer(bundleA.getFile("src/A.java"), new StringBuffer("class A {\n}\n"));
		Utils.writeBuffer(bundleA.getFile("about.html"), new StringBuffer("about\n"));

		Utils.generateBundleManifest(bundleB, "bundleB", "1.0.0", null);
		buildProperties = new Properties();
		buildProperties.put("src.includes", "about.html");
		Utils.generatePluginBuildProperties(bundleB, buildProperties);
		Utils.writeBuffer(bundleB.getFile("src/B.java"), new StringBuffer("class B {\n}\n"));
		Utils.writeBuffer(bundleB.getFile("about.html"), new StringBuffer("about\n"));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "sdk");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		Set entries = new HashSet();
		entries.clear();
		entries.add("A.java");
		entries.add("about.html");
		assertZipContents(buildFolder, "tmp/eclipse/plugins/bundleA.source_1.0.0.jar", entries);

		entries.clear();
		entries.add("B.java");
		entries.add("about.html");
		assertZipContents(buildFolder, "tmp/eclipse/plugins/bundleB.source_1.0.0.jar", entries);
	}

	public void testIndividualSourceBundles_2() throws Exception {
		IFolder buildFolder = newTest("individualSourceBundles2");

		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateBundle(bundleA, "bundleA");
		File src = new File(bundleA.getLocation().toFile(), "src/A.java");
		src.getParentFile().mkdir();
		FileOutputStream outputStream = new FileOutputStream(src);
		outputStream.write("class A {\n}\n".getBytes());
		outputStream.close();

		//generate an SDK feature
		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"bundleA", "bundleA.source"});
		Properties properties = new Properties();
		properties.put("generate.plugin@bundleA.source", "bundleA");
		Utils.storeBuildProperties(sdk, properties);

		Utils.generateAllElements(buildFolder, "sdk");
		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("individualSourceBundles", "true");
		buildProperties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runBuild(buildFolder);

		IFolder plugins = buildFolder.getFolder("tmp/eclipse/plugins");
		IFolder binaryA = plugins.getFolder("bundleA_1.0.0");
		IFolder binaryASource = plugins.getFolder("bundleA.source_1.0.0");
		assertResourceFile(binaryA, "A.class");
		assertResourceFile(binaryASource, "A.java");

		IFile manifestFile = plugins.getFile("bundleA.source_1.0.0/META-INF/MANIFEST.MF");
		InputStream contents = manifestFile.getContents();
		Manifest manifest = new Manifest(contents);
		contents.close();
		Attributes attr = manifest.getMainAttributes();
		assertEquals(attr.getValue("Bundle-Version"), "1.0.0");
		assertEquals(attr.getValue("Bundle-SymbolicName"), "bundleA.source");
		assertTrue(attr.getValue("Eclipse-SourceBundle").startsWith("bundleA;version=\"1.0.0\""));
	}

	public void test243475_243227() throws Exception {
		IFolder buildFolder = newTest("243475");
		IFolder bundleFolder = Utils.createFolder(buildFolder, "plugins/a.bundle");
		IFolder sdkFolder = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateBundleManifest(bundleFolder, "a.bundle", "1.0.0", null);
		Properties props = new Properties();
		props.put("src.includes", "about.html");
		Utils.generatePluginBuildProperties(bundleFolder, props);
		//add some source to a.bundle
		File src = new File(bundleFolder.getLocation().toFile(), "src/a.java");
		src.getParentFile().mkdir();
		FileOutputStream stream = new FileOutputStream(src);
		stream.write("//L33T CODEZ\n".getBytes());
		stream.close();
		//add the about.html
		File about = new File(bundleFolder.getLocation().toFile(), "about.html");
		stream = new FileOutputStream(about);
		stream.write("about\n".getBytes());
		stream.close();

		Utils.generateFeature(buildFolder, "rcp", null, new String[] {"a.bundle"}, "1.0.0.qualifier");

		Utils.generateFeature(buildFolder, "sdk", new String[] {"rcp", "rcp.source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@rcp.source", "rcp");
		Utils.storeBuildProperties(sdkFolder, properties);

		Utils.generateAllElements(buildFolder, "sdk");
		Properties buildProperties = BuildConfiguration.getBuilderProperties(buildFolder);
		buildProperties.put("archivesFormat", "*,*,*-folder");
		buildProperties.put("forceContextQualifier", "123");
		Utils.storeBuildProperties(buildFolder, buildProperties);
		runBuild(buildFolder);

		assertResourceFile(buildFolder, "tmp/eclipse/plugins/rcp.source_1.0.0.123/src/a.bundle_1.0.0/about.html");
		assertResourceFile(buildFolder, "tmp/eclipse/plugins/rcp.source_1.0.0.123/src/a.bundle_1.0.0/src.zip");

		//build again using the binaries output from the first build
		IFolder build2 = Utils.createFolder(buildFolder, "2");

		//top level feature must be in buildDirectory
		Utils.createFolder(build2, "features");
		sdkFolder.move(new Path("../2/features/sdk"), true, null);

		String oldBuild = buildFolder.getLocation().toOSString();
		Utils.generateAllElements(build2, "sdk");
		buildProperties = BuildConfiguration.getBuilderProperties(build2);
		buildProperties.put("archivesFormat", "*,*,*-folder");
		buildProperties.put("pluginPath", oldBuild + "/tmp/eclipse" + File.pathSeparator + oldBuild + "/features/rcp");
		buildProperties.put("forceContextQualifier", "124");
		Utils.storeBuildProperties(build2, buildProperties);
		runBuild(build2);

		assertResourceFile(build2, "tmp/eclipse/plugins/rcp.source_1.0.0.124/src/a.bundle_1.0.0/about.html");
		assertResourceFile(build2, "tmp/eclipse/plugins/rcp.source_1.0.0.124/src/a.bundle_1.0.0/src.zip");
	}

	public void testBug247007_247027() throws Exception {
		IFolder buildFolder = newTest("247007");
		IFolder sdkFolder = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "sdk", new String[] {"sdk.source"}, new String[] {"org.apache.ant"});
		Properties properties = new Properties();
		properties.put("generate.feature@sdk.source", "sdk,feature@org.eclipse.rcp");
		Utils.storeBuildProperties(sdkFolder, properties);

		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
		properties.put("filteredDependencyCheck", "true");
		generateScripts(buildFolder, properties);

		//if we failed to account for the source features, then osgi would not have been in the state and 
		//org.apache.ant would have failed to resolve and generateScripts would have thrown an exception
	}

	public void testBug257761() throws Exception {
		IFolder buildFolder = newTest("257761");
		IFolder a1 = Utils.createFolder(buildFolder, "plugins/a1");
		IFolder a2 = Utils.createFolder(buildFolder, "plugins/a2");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "sdk", new String[] {"sdk.source"}, new String[] {"a;version=1.0.0", "a;version=2.0.0"});
		Properties properties = new Properties();
		properties.put("generate.feature@sdk.source", "sdk");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(sdk, properties);

		Utils.generateBundleManifest(a1, "a", "1.0.0", null);
		Utils.generatePluginBuildProperties(a1, null);
		Utils.writeBuffer(a1.getFile("src/a.java"), new StringBuffer("class a{}"));

		Utils.generateBundleManifest(a2, "a", "2.0.0", null);
		Utils.generatePluginBuildProperties(a2, null);
		Utils.writeBuffer(a2.getFile("src/a.java"), new StringBuffer("class a{}"));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "sdk");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);

		runBuild(buildFolder);
		assertResourceFile(buildFolder, "tmp/eclipse/plugins/a.source_1.0.0.jar");
		assertResourceFile(buildFolder, "tmp/eclipse/plugins/a.source_2.0.0.jar");
	}

	public void testBug272543() throws Exception {
		IFolder root = newTest("272543");
		IFolder buildFolder = Utils.createFolder(root, "build1");
		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"bundleA", "bundleA.source;unpack=false"});
		Properties properties = new Properties();
		properties.put("generate.plugin@bundleA.source", "bundleA");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(sdk, properties);

		Utils.generateBundleManifest(bundleA, "bundleA", "1.0.0", null);
		Properties buildProperties = new Properties();
		buildProperties.put("src.includes", "about.html");
		Utils.generatePluginBuildProperties(bundleA, buildProperties);
		Utils.writeBuffer(bundleA.getFile("src/A.java"), new StringBuffer("class A {\n}\n"));
		Utils.writeBuffer(bundleA.getFile("about.html"), new StringBuffer("about\n"));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "sdk");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IFolder build2 = Utils.createFolder(root, "build2");
		IFolder sdk2 = Utils.createFolder(build2, "features/sdk2");
		Utils.generateFeature(build2, "sdk2", null, new String[] {"bundleA", "bundleA.source;unpack=false"});
		properties = new Properties();
		properties.put("generate.plugin@bundleA.source", "bundleA");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(sdk2, properties);

		properties = BuildConfiguration.getBuilderProperties(build2);
		properties.put("topLevelElementId", "sdk2");
		properties.put("baseLocation", buildFolder.getFolder("tmp/eclipse").getLocation().toOSString());
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(build2, properties);
		runBuild(build2);
	}

	public void testBug290828() throws Exception {
		IFolder buildFolder = newTest("290828");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "f", null, new String[] {"org.eclipse.cvs"});
		Utils.generateFeature(buildFolder, "sdk", new String[] {"f", "f.source"}, null);

		Properties properties = new Properties();
		properties.put("generate.feature@f.source", "f");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(sdk, properties);

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "sdk");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);
		runBuild(buildFolder);

		IFile feature = buildFolder.getFile("tmp/eclipse/features/f.source_1.0.0/feature.xml");
		assertResourceFile(feature);
		BuildTimeFeatureFactory factory = new BuildTimeFeatureFactory();
		BuildTimeFeature model = factory.parseBuildFeature(feature.getLocationURI().toURL());
		FeatureEntry[] included = model.getPluginEntries();
		assertEquals(1, included.length);
		for (int i = 0; i < included.length; i++) {
			assertResourceFile(buildFolder, "tmp/eclipse/plugins/" + included[i].getId() + "_" + included[i].getVersion() + ".jar");
		}
	}

	public void testbug302941() throws Exception {
		IFolder buildFolder = newTest("302941");

		IFolder bundleA = Utils.createFolder(buildFolder, "plugins/bundleA");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		Utils.generateFeature(buildFolder, "sdk", null, new String[] {"bundleA", "bundleA.source;unpack=false"});
		Properties properties = new Properties();
		properties.put("generate.plugin@bundleA.source", "bundleA");
		properties.put("individualSourceBundles", "true");
		Utils.storeBuildProperties(sdk, properties);

		Utils.generateBundleManifest(bundleA, "bundleA", "1.0.0", null);
		Properties buildProperties = new Properties();
		buildProperties.put("src.includes", "about.html");
		Utils.generatePluginBuildProperties(bundleA, buildProperties);
		Utils.writeBuffer(bundleA.getFile("src/A.java"), new StringBuffer("class A {\n}\n"));
		Utils.writeBuffer(bundleA.getFile("about.html"), new StringBuffer("about\n"));

		properties = BuildConfiguration.getBuilderProperties(buildFolder);
		properties.put("topLevelElementId", "sdk");
		properties.put("baseLocation", "");
		properties.put("archivesFormat", "*,*,*-folder");
		Utils.storeBuildProperties(buildFolder, properties);

		StringBuffer buildAll = new StringBuffer();
		buildAll.append("<project default=\"main\">                                                                  \n");
		buildAll.append("   <target name=\"main\" >                                                                  \n");
		buildAll.append("      <property name=\"builder\" value=\"${basedir}\" />                                    \n");
		buildAll.append("      <ant antfile=\"build.xml\" dir=\"${eclipse.pdebuild.scripts}\" target=\"generate\" /> \n");
		buildAll.append("      <ant antfile=\"build.xml\" dir=\"${eclipse.pdebuild.scripts}\" target=\"generate\" /> \n");
		buildAll.append("   </target>                                                                                \n");
		buildAll.append("</project>                                                                                  \n");

		IFile buildXml = buildFolder.getFile("buildAll.xml");
		Utils.writeBuffer(buildXml, buildAll);

		runAntScript(buildXml.getLocation().toOSString(), new String[] {"main"}, buildFolder.getLocation().toOSString(), null);

		IFile buildScript = buildFolder.getFile("plugins/bundleA.source_1.0.0/build.xml");
		Project antProject = assertValidAntScript(buildScript);
		Target publishBinParts = (Target) antProject.getTargets().get("publish.bin.parts");
		assertNotNull(publishBinParts);
		Object child = AntUtils.getFirstChildByName(publishBinParts, "ant");
		assertNotNull(child);
		assertTrue(child instanceof Ant);
	}
}
