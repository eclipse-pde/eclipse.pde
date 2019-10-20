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

import java.io.File;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;
import org.junit.Assume;
import org.junit.Test;

public class ProfileContainerTests extends AbstractTargetTest {

	@Test
	public void testBundleResolutionWithConfigIni() {
		File bundlesInfo = new File(new ProfileBundleContainer("${eclipse.home}", null).getConfigurationLocation(), SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);
		Assume.assumeFalse("Skip test when using regular p2 configurator", bundlesInfo.isFile());
		ITargetDefinition defaultDefinition = getTargetService().newDefaultTarget();
		defaultDefinition.resolve(new NullProgressMonitor());
		assertTrue(defaultDefinition.getBundles().length > 10);
	}
}
