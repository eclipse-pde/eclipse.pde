/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Platform;
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

	public void testBug206679() throws Exception {
		IFolder buildFolder = newTest("206679");
		IFolder sdk = Utils.createFolder(buildFolder, "features/sdk");

		//generate an SDK feature
		Utils.generateFeature(buildFolder, "sdk", new String[] {"org.eclipse.jdt", "jdt.source"}, null);
		Properties properties = new Properties();
		properties.put("generate.feature@jdt.source", "org.eclipse.jdt");
		Utils.storeBuildProperties(sdk, properties);

		Properties props = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
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

		String fragmentName = "rcp.source." + os + "." + ws + "." + arch;
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

		entries.clear();
		entries.add("about.html");
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
		Manifest manifest = new Manifest(manifestFile.getContents());
		Attributes attr = manifest.getMainAttributes();
		assertEquals(attr.getValue("Bundle-Version"), "1.0.0");
		assertEquals(attr.getValue("Bundle-SymbolicName"), "bundleA.source");
		assertTrue(attr.getValue("Eclipse-SourceBundle").startsWith("bundleA;version=\"1.0.0\""));
	}

}
