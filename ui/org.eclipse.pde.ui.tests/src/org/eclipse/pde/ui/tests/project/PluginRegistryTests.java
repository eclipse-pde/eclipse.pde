/*******************************************************************************
 * Copyright (c) 2010, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * Tests for plug-in searching
 *
 * @since 3.6
 */
public class PluginRegistryTests extends PluginRegistryTestsMinimal {

	@Test
	public void testMatchEquivalent() {
		Version testsVersion = FrameworkUtil.getBundle(getClass()).getVersion();
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.ui.tests",
				String.format("%s.%s.%s", testsVersion.getMajor(), testsVersion.getMinor(), testsVersion.getMicro()),
				IMatchRules.EQUIVALENT,
				null);
		assertNotNull("NOTE: This test might also fail because the version of the bundle got changed.", model);
		assertEquals("org.eclipse.pde.ui.tests", model.getPluginBase().getId());
	}
}
