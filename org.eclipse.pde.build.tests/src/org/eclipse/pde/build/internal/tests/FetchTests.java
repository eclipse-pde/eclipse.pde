/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.build.internal.tests;

import java.net.URL;
import java.util.Properties;
import java.util.jar.Attributes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.tests.BuildConfiguration;
import org.eclipse.pde.build.tests.PDETestCase;

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
		assertLogContainsLine(buildFolder.getFile("log.log"), "[eclipse.fetch] Could not retrieve feature.xml and/or build.properties:");
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

		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.1.v20070417.jar");
		assertResourceFile(buildFolder, "plugins/com.ibm.icu.base_3.6.0.20061215.jar");
	}
}
