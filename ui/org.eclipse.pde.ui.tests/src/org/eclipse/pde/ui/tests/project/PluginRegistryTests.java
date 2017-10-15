/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.pde.core.plugin.*;
import org.junit.Test;

/**
 * Tests for plug-in searching
 *
 * @since 3.6
 */
public class PluginRegistryTests extends PluginRegistryTestsMinimal {

	@Test
	public void testMatchEquivalent() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.ui.tests", "3.10.100",
				IMatchRules.EQUIVALENT,
				null);
		assertNotNull("NOTE: This test might also fail because the version of the bundle got changed.", model);
		assertEquals("org.eclipse.pde.ui.tests", model.getPluginBase().getId());
	}
}
