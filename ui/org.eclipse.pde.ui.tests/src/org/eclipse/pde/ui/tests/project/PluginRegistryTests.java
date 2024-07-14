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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.VersionMatchRule;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * Tests for plug-in searching
 *
 * @since 3.6
 */
public class PluginRegistryTests {

	@Test
	public void testMatchNone() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", null, (VersionMatchRule) null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testMatchGreaterOrEqual() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", "3.0.0",
				VersionMatchRule.GREATER_OR_EQUAL);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testMatchPerfect() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", "3.0.0", VersionMatchRule.PERFECT);
		assertNull(model);
	}

	@Test
	public void testMatchCompatible() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", "3.6.0",
				VersionMatchRule.COMPATIBLE);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testMatchCompatibleNone() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.core", "2.6.0", VersionMatchRule.COMPATIBLE);
		assertNull(model);
	}

	@Test
	public void testMatchPrefix() {
		@SuppressWarnings("removal") // Test special behavior of old API
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse", "3.6.0", IMatchRules.PREFIX, null);
		// prefix rule is not supported by this API, should return null
		assertNull(model);
	}

	@Test
	public void testRangeNone() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testOverlapRange() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", new VersionRange("[2.0.0,4.0.0)"));
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testMinRange() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", new VersionRange("3.0.0"));
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testUnmatchedRange() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.core", new VersionRange("[1.0.0,2.0.0)"));
		assertNull(model);
	}

	@Test
	public void testRangeWithFilterMatch() {
		IPluginModelBase model = PluginRegistry.findModels("org.eclipse.jdt.debug", new VersionRange("[2.0.0,4.0.0)"))
				.filter(m -> m.getPluginBase().getId().startsWith("org.eclipse")).findFirst().orElse(null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}

	@Test
	public void testRangeWithFilterNoMatch() {
		IPluginModelBase model = PluginRegistry.findModels("org.eclipse.jdt.debug", new VersionRange("[2.0.0,4.0.0)"))
				.filter(m -> m.getPluginBase().getId().startsWith("xyz")).findFirst().orElse(null);
		assertNull(model);
	}

	@Test
	public void testSingleRangeMatch() {
		List<IPluginModelBase> models = PluginRegistry
				.findModels("org.eclipse.jdt.debug", new VersionRange("[1.0.0,4.0.0)")).toList();
		assertNotNull(models);
		assertEquals(1, models.size());
		assertEquals("org.eclipse.jdt.debug", models.get(0).getPluginBase().getId());
	}

	@Test
	public void testWorkspaceOverTarget() throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("org.junit");
		try {
			IBundleProjectDescription description = ProjectCreationTests.getBundleProjectService()
					.getDescription(project);
			description.setSymbolicName("org.junit");
			description.setBundleVersion(new Version("4.3.2"));
			description.setBundleVendor("bogus");
			description.apply(null);

			waitForBuild();

			List<IPluginModelBase> models = PluginRegistry.findModels("org.junit", new VersionRange("[3.8.2,4.8.2]"))
					.toList();
			assertNotNull(models);
			assertEquals(1, models.size());
			assertEquals("org.junit", models.get(0).getPluginBase().getId());
			assertEquals(project, models.get(0).getUnderlyingResource().getProject());

		} finally {
			if (project.exists()) {
				project.delete(true, null);
				waitForBuild();
			}
		}
	}

	@Test
	public void testMatchEquivalent() {
		Version testsVersion = FrameworkUtil.getBundle(PluginRegistryTests.class).getVersion();
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.ui.tests",
				String.format("%s.%s.%s", testsVersion.getMajor(), testsVersion.getMinor(), testsVersion.getMicro()),
				VersionMatchRule.EQUIVALENT);
		assertNotNull("NOTE: This test might also fail because the version of the bundle got changed.", model);
		assertEquals("org.eclipse.pde.ui.tests", model.getPluginBase().getId());
	}

	/**
	 * Wait for builds to complete
	 */
	public static void waitForBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}
}
