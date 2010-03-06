/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import junit.framework.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.PluginRegistry.PluginFilter;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.osgi.framework.Version;

/**
 * Tests for plug-in searching
 * 
 * @since 3.6
 */
public class PluginRegistryTests extends TestCase {

	public static Test suite() {
		return new TestSuite(PluginRegistryTests.class);
	}
	
	public void testMatchNone() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", null, IMatchRules.NONE, null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}
	
	public void testMatchGreaterOrEqual() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", "3.0.0", IMatchRules.GREATER_OR_EQUAL, null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}
	
	public void testMatchPerfect() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", "3.0.0", IMatchRules.PERFECT, null);
		assertNull(model);
	}
	
	public void testMatchCompatible() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.core", "3.6.0", IMatchRules.COMPATIBLE, null);
		assertNotNull(model);
		assertEquals("org.eclipse.pde.core", model.getPluginBase().getId());
	}
	
	public void testMatchCompatibleNone() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.core", "2.6.0", IMatchRules.COMPATIBLE, null);
		assertNull(model);
	}
	
	public void testMatchEquivalent() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.core", "3.6.0", IMatchRules.EQUIVALENT, null);
		assertNotNull(model);
		assertEquals("org.eclipse.pde.core", model.getPluginBase().getId());
	}
	
	public void testMatchPrefix() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse", "3.6.0", IMatchRules.PREFIX, null);
		// prefix rule is not supported by this API, should return null
		assertNull(model);
	}
	
	public void testRangeNone() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", null, null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}
	
	public void testOverlapRange() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", new VersionRange("[2.0.0,4.0.0)"), null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}
	
	public void testMinRange() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", new 	VersionRange("3.0.0"), null);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}
	
	public void testUnmatchedRange() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.pde.core", new VersionRange("[1.0.0,2.0.0)"), null);
		assertNull(model);
	}
	
	public void testRangeWithFilterMatch() {
		PluginFilter filter = new PluginFilter() {
			/* (non-Javadoc)
			 * @see org.eclipse.pde.core.plugin.PluginRegistry.PluginFilter#accept(org.eclipse.pde.core.plugin.IPluginModelBase)
			 */
			public boolean accept(IPluginModelBase model) {
				IPluginBase base = model.getPluginBase();
				if (base != null) {
					String id = base.getId();
					if (id != null) {
						return id.startsWith("org.eclipse");
					}
				}
				return false;
			}
		};
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", new VersionRange("[2.0.0,4.0.0)"), filter);
		assertNotNull(model);
		assertEquals("org.eclipse.jdt.debug", model.getPluginBase().getId());
	}
	
	public void testRangeWithFilterNoMatch() {
		PluginFilter filter = new PluginFilter() {
			/* (non-Javadoc)
			 * @see org.eclipse.pde.core.plugin.PluginRegistry.PluginFilter#accept(org.eclipse.pde.core.plugin.IPluginModelBase)
			 */
			public boolean accept(IPluginModelBase model) {
				IPluginBase base = model.getPluginBase();
				if (base != null) {
					String id = base.getId();
					if (id != null) {
						return id.startsWith("xyz");
					}
				}
				return false;
			}
		};
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.jdt.debug", new VersionRange("[2.0.0,4.0.0)"), filter);
		assertNull(model);
	}
	
	public void testSingleRangeMatch() {
		IPluginModelBase[] models = PluginRegistry.findModels("org.eclipse.pde.core", new VersionRange("[1.0.0,4.0.0)"), null);
		assertNotNull(models);
		assertEquals(1, models.length);
		assertEquals("org.eclipse.pde.core", models[0].getPluginBase().getId());
	}
	
	public void testMutliMatches() {
		IPluginModelBase[] models = PluginRegistry.findModels("org.junit", new VersionRange("[3.8.2,4.8.2]"), null);
		assertNotNull(models);
		assertEquals(2, models.length);
		assertEquals("org.junit", models[0].getPluginBase().getId());
		assertEquals("org.junit", models[1].getPluginBase().getId());
	}
	
	public void testWorkspaceOverTarget() throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("org.junit");
		try {
			IBundleProjectDescription description = ProjectCreationTests.getBundleProjectService().getDescription(project);
			description.setSymbolicName("org.junit");
			description.setBundleVersion(new Version("4.3.2"));
			description.setBundleVendor("bogus");
			description.apply(null);
			
			waitForBuild();
			
			IPluginModelBase[] models = PluginRegistry.findModels("org.junit", new VersionRange("[3.8.2,4.8.2]"), null);
			assertNotNull(models);
			assertEquals(1, models.length);
			assertEquals("org.junit", models[0].getPluginBase().getId());
			assertEquals(project, models[0].getUnderlyingResource().getProject());
			
		} finally {
			if (project.exists()) {
				project.delete(true, null);
				waitForBuild();
			}
		}
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
