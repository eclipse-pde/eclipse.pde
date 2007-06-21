/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.jar.Manifest;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;
import org.eclipse.pde.internal.build.FeatureGenerator;
import org.eclipse.update.core.model.FeatureModelFactory;
import org.osgi.framework.FrameworkUtil;

public class SourceTests extends PDETestCase {

	public static Test suite() {
		return new TestSuite(SourceTests.class);
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
		assertZipContents(buildFolder, "I.TestBuild/a.feature.sdk.zip", entries);

		entries.add("eclipse/features/a.feature_1.0.0/feature.xml");
		entries.add("eclipse/plugins/a.plugin_1.0.0.jar");
		assertZipContents(buildFolder, "I.TestBuild/a.feature.zip", entries);
	}

	// test that generated source fragments have a proper platform filter
	public void testBug184517() throws Exception {
		IFolder buildFolder = newTest("184517");

		IFolder features = buildFolder.getFolder("features");;
		features.create(true, true, null);
		
		//generate an SDK feature
		FeatureGenerator generator = new FeatureGenerator();
		generator.setIncludeLaunchers(false);
		generator.setVerify(false);
		generator.setFeatureId("sdk");
		generator.setFeatureList(new String[] {"org.eclipse.rcp", "org.eclipse.rcp.source"});
		generator.setWorkingDirectory(buildFolder.getLocation().toOSString());
		generator.generate();
		
		Properties properties = new Properties();
		properties.put("generate.feature@org.eclipse.rcp.source", "org.eclipse.rcp");
		IFolder sdk = features.getFolder("sdk");
		Utils.storeBuildProperties(sdk, properties);
		
		String os = Platform.getOS();
		String ws = Platform.getWS();
		String arch = Platform.getOSArch();

		//getScriptGenerationProperties sets buildDirectory to buildFolder by default
		properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "sdk");
		properties.put("configs", os + "," + ws + "," + arch);
		generateScripts(buildFolder, properties);

		String fragmentName = "org.eclipse.rcp.source." + os + "." + ws + "." + arch;
		IFolder fragment = buildFolder.getFolder("plugins/" + fragmentName);

		// check the manifest for a correct platform filter
		assertResourceFile(fragment, "META-INF/MANIFEST.MF");
		InputStream stream = new BufferedInputStream(fragment.getFile("META-INF/MANIFEST.MF").getLocationURI().toURL().openStream());
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
	public void testbug184920() throws Exception {
		//the provided resource features/a.feature/feature.xml contains &lt;foo!&gt; 
		//which must be handled properly
		IFolder buildFolder = newTest("184920");

		Properties properties = BuildConfiguration.getScriptGenerationProperties(buildFolder, "feature", "a.feature.sdk");
		generateScripts(buildFolder, properties);
		
		assertResourceFile(buildFolder, "features/a.feature.source/feature.xml");
		IFile feature = buildFolder.getFile("features/a.feature.source/feature.xml");

		FeatureModelFactory factory = new FeatureModelFactory();
		InputStream stream = new BufferedInputStream(feature.getLocationURI().toURL().openStream());
		try {
			//this will throw an exception if feature.xml is not valid
			factory.parseFeature(stream);
		} finally {
			stream.close();
		}
	}
}
