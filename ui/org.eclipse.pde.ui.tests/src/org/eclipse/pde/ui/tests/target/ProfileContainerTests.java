/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;
import org.junit.Assume;
import org.junit.Test;

public class ProfileContainerTests extends AbstractTargetTest {

	@Test
	public void testBundleResolutionWithConfigIni() {
		File bundlesInfo = new File(new ProfileBundleContainer("${eclipse.home}", null).getConfigurationLocation(),
				SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);
		Assume.assumeFalse("Skip test when using regular p2 configurator", bundlesInfo.isFile());
		ITargetDefinition defaultDefinition = getTargetService().newDefaultTarget();
		defaultDefinition.resolve(new NullProgressMonitor());
		assertTrue(defaultDefinition.getBundles().length > 10);
	}

	@Test
	public void testParseBundleInfoFromConfigIni() {

		Properties configIni = new Properties();
		configIni.put("osgi.bundles", absoluteFile("plugins/some.bundle").toURI() + ","//
				+ "reference:" + absoluteFile("plugins/some.bundle_startlevel").toURI() + "@1:start");

		Collection<File> parsedBundles = ProfileBundleContainer.parseBundlesFromConfigIni(configIni, new File("."));
		assertEquals(Arrays.asList( //
				absoluteFile("plugins/some.bundle"), //
				absoluteFile("plugins/some.bundle_startlevel")), //
				parsedBundles);
	}

	@Test
	public void testParseBundleInfoFromConfigIni_relative() {
		Properties configIni = new Properties();
		configIni.put("osgi.bundles", "reference:file:plugins/some.bundle," //
				+ "reference:file:plugins/some.bundle_startlevel@1:start," //
				+ "reference:" + absoluteFile("absolute.bundle").toURI());

		Collection<File> parsedBundles = ProfileBundleContainer.parseBundlesFromConfigIni(configIni, new File("."));
		assertEquals(Arrays.asList( //
				new File("plugins/some.bundle"), //
				new File("plugins/some.bundle_startlevel"), //
				absoluteFile("absolute.bundle")), //
				parsedBundles);
	}

	@Test
	public void testParseBundleInfoFromConfigIni_relativeToFramework() {
		Properties configIni = new Properties();
		configIni.put("osgi.bundles", "reference:file:some.bundle," //
				+ "reference:file:some.bundle_startlevel@1:start," //
				+ "reference:" + absoluteFile("absolute.bundle").toURI());
		configIni.put("osgi.framework", "file:plugins/o.e.osgi.jar");

		Collection<File> parsedBundles = ProfileBundleContainer.parseBundlesFromConfigIni(configIni, new File("/home"));
		assertEquals(Arrays.asList( //
				new File("/home/plugins/o.e.osgi.jar"), //
				new File("/home/plugins/some.bundle"), //
				new File("/home/plugins/some.bundle_startlevel"), //
				absoluteFile("absolute.bundle")), //
				parsedBundles);
	}

	@Test
	public void testParseBundleInfoFromConfigIni_relativeToAbsoluteFramework() {
		Properties configIni = new Properties();
		configIni.put("osgi.bundles", "reference:file:some.bundle," //
				+ "reference:file:some.bundle_startlevel@1:start");
		configIni.put("osgi.framework", absoluteFile("plugins/o.e.osgi.jar").toURI().toString());

		Collection<File> parsedBundles = ProfileBundleContainer.parseBundlesFromConfigIni(configIni, new File("/home"));
		assertEquals(Arrays.asList( //
				absoluteFile("plugins/o.e.osgi.jar"), //
				absoluteFile("plugins/some.bundle"), //
				absoluteFile("plugins/some.bundle_startlevel")), //
				parsedBundles);
	}

	private static File absoluteFile(String path) {
		return new File(path).getAbsoluteFile();
	}
}
