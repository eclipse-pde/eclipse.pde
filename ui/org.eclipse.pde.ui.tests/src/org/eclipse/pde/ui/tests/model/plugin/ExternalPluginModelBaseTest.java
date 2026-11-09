/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.junit.Test;

/**
 * Tests the loading of external plugin and fragment models from a stream.
 */
public class ExternalPluginModelBaseTest {

	@Test
	public void testLoadsPluginModel() throws CoreException {
		ExternalPluginModelBase model = createAndLoad("<plugin id=\"p\"/>", false, "/install/location");

		assertTrue(model instanceof ExternalPluginModel);
		assertTrue(model.isLoaded());
		assertEquals("p", model.getPluginBase().getId());
		assertEquals("/install/location", model.getInstallLocation());
	}

	@Test
	public void testLoadsFragmentModel() throws CoreException {
		ExternalPluginModelBase model = createAndLoad("<fragment id=\"f\" plugin-id=\"p\"/>", true, null);

		assertTrue(model instanceof ExternalFragmentModel);
		assertTrue(model.isLoaded());
		assertEquals("p", ((IFragment) model.getPluginBase()).getPluginId());
		assertNull(model.getInstallLocation());
	}

	@Test
	public void testReturnsUnloadedModelForMalformedContent() throws CoreException {
		ExternalPluginModelBase model = createAndLoad("<plugin", false, null);

		assertFalse(model.isLoaded());
	}

	private static ExternalPluginModelBase createAndLoad(String xml, boolean fragment, String installLocation)
			throws CoreException {
		return ExternalPluginModelBase.createAndLoad(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
				fragment, installLocation);
	}
}
