/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.model.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.BundleVersionRange;
import org.eclipse.pde.api.tools.internal.RequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.junit.Test;

/**
 * @since 1.0.0
 */
public class ComponentManifestTests {
	@Test
	public void testComponentManifest() throws CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-manifests"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue("Missing manifest directory", file.exists()); //$NON-NLS-1$
		IApiBaseline baseline = TestSuiteHelper.newApiBaseline("test", TestSuiteHelper.getEEDescriptionFile()); //$NON-NLS-1$
		try {
			IApiComponent component = ApiModelFactory.newApiComponent(baseline, file.getAbsolutePath());
			baseline.addApiComponents(new IApiComponent[] { component });
			assertEquals("Id: ", "org.eclipse.debug.ui", component.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("Name: ", "Debug Platform UI", component.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("Version: ", "3.3.100", component.getVersion()); //$NON-NLS-1$ //$NON-NLS-2$
			String[] envs = component.getExecutionEnvironments();
			assertEquals("Wrong number of execution environments", 1, envs.length); //$NON-NLS-1$
			assertEquals("Version: ", "J2SE-1.4", envs[0]); //$NON-NLS-1$ //$NON-NLS-2$

			IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
			assertEquals("Wrong number of required components", 11, requiredComponents.length); //$NON-NLS-1$

			List<RequiredComponentDescription> reqs = new ArrayList<>();
			reqs.add(new RequiredComponentDescription("org.eclipse.core.expressions", new BundleVersionRange("(3.3.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.core.variables", new BundleVersionRange("[3.2.0,4.0.0]"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.ui", new BundleVersionRange("[3.3.0,4.0.0]"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.ui.console", new BundleVersionRange("[3.2.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.help", new BundleVersionRange("3.3.0"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.debug.core", new BundleVersionRange("3.4.0"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.jface.text", new BundleVersionRange("[3.3.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.ui.workbench.texteditor", new BundleVersionRange("[3.3.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.ui.ide", new BundleVersionRange("[3.3.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.ui.editors", new BundleVersionRange("[3.3.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$
			reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange("[3.3.0,4.0.0)"))); //$NON-NLS-1$ //$NON-NLS-2$

			for (int i = 0; i < reqs.size(); i++) {
				assertEquals("Wrong required component", reqs.get(i), requiredComponents[i]); //$NON-NLS-1$
			}
		} finally {
			baseline.dispose();
		}
	}

	@Test
	public void testReExport() throws CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-manifests"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue("Missing manifest directory", file.exists()); //$NON-NLS-1$
		IApiBaseline baseline = TestSuiteHelper.newApiBaseline("test", TestSuiteHelper.getEEDescriptionFile()); //$NON-NLS-1$
		try {
			IApiComponent component = ApiModelFactory.newApiComponent(baseline, file.getAbsolutePath());
			baseline.addApiComponents(new IApiComponent[] { component });

			boolean debugCoreExport = false;
			boolean others = false;
			IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
			for (IRequiredComponentDescription description : requiredComponents) {
				if (description.getId().equals("org.eclipse.debug.core")) { //$NON-NLS-1$
					debugCoreExport = description.isExported();
				} else {
					others = others || description.isExported();
				}
			}
			assertTrue("org.eclipse.debug.core should be re-exported", debugCoreExport); //$NON-NLS-1$
			assertFalse("Other components should not be re-exported", others); //$NON-NLS-1$
		} finally {
			baseline.dispose();
		}
	}
}
