/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.pde.core.plugin.*;

/**
 * Tests for plug-in searching
 *
 * @since 3.6
 */
public class PluginRegistryTests extends PluginRegistryTestsMinimal {
	public static Test suite() {
		return new TestSuite(PluginRegistryTests.class);
	}

	public void testMatchEquivalent() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.ui.tests", "3.2.0", IMatchRules.EQUIVALENT,
				null);
		assertNotNull(model);
		assertEquals("org.eclipse.pde.ui.tests", model.getPluginBase().getId());
	}
}
