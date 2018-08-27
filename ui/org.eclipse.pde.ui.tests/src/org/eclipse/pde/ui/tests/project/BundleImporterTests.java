/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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

import java.util.Map;
import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.importing.provisional.IBundleImporter;

/**
 * Tests for bundle importer extensions.
 *
 * @since 3.6
 */
public class BundleImporterTests extends TestCase {

	private static final String CVS_IMPORTER = "org.eclipse.team.core.cvs.importer";

	/**
	 * Tests that a import description can be created for a known plug-in
	 * @throws CoreException
	 */
	public void testGetImportDescriptions() throws CoreException {
		String bundleId = "org.eclipse.jdt.core";
		String expectedURL = "scm:cvs:pserver:dev.eclipse.org:/cvsroot/eclipse:org.eclipse.jdt.core";
		ModelEntry plugin = PluginRegistry.findEntry(bundleId);
		IPluginModelBase[] models = new IPluginModelBase[] { plugin.getModel()};
		Map<IBundleImporter, ScmUrlImportDescription[]> descMap = ((BundleProjectService) BundleProjectService
				.getDefault()).getImportDescriptions(models);
		assertEquals(1, descMap.size());
		IBundleImporter	importer = (IBundleImporter) descMap.keySet().toArray()[0];
		assertEquals(CVS_IMPORTER, importer.getId());
		ScmUrlImportDescription[] descriptions = descMap.get(importer);
		assertEquals(1, descriptions.length);
		ScmUrlImportDescription description = descriptions[0];
		assertTrue("Incorrect URL Length: " + description.getUrl(),description.getUrl().length() >= expectedURL.length());
		assertEquals(expectedURL,description.getUrl().substring(0,expectedURL.length()));
		assertEquals(bundleId, description.getProject());
		assertTrue(description.getProperty(BundleProjectService.PLUGIN) instanceof IPluginModelBase);
		assertEquals(bundleId, ((IPluginModelBase)description.getProperty(BundleProjectService.PLUGIN)).getBundleDescription().getSymbolicName());
		assertTrue(description.getProperty(BundleProjectService.BUNDLE_IMPORTER) instanceof IBundleImporter);
	}

	/**
	 * Tests that the team API returns all known bundle importers
	 */
	public void testBundleImporters() {
		IBundleImporter[] importers = Team.getBundleImporters();
		assertEquals(1, importers.length);
		assertEquals(CVS_IMPORTER, importers[0].getId());
		assertEquals("CVS Bundle Importer", importers[0].getName());
	}

}
