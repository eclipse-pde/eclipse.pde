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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.internal.BundleVersionRange;
import org.eclipse.pde.api.tools.internal.RequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.junit.jupiter.api.Test;

/**
 * @since 1.0.0
 */
public class ComponentManifestTests {
	@Test
	public void testComponentManifest() throws CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-manifests"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue(file.exists(), "Missing manifest directory"); //$NON-NLS-1$
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test", TestSuiteHelper.getEEDescription(), null); //$NON-NLS-1$
		try {
			IApiComponent component = ApiModelFactory.newApiComponent(baseline, file.getAbsolutePath());
			baseline.addApiComponents(new IApiComponent[] { component });
			assertEquals("org.eclipse.debug.ui", component.getSymbolicName(), "Id: "); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("Debug Platform UI", component.getName(), "Name: "); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("3.3.100", component.getVersion(), "Version: "); //$NON-NLS-1$ //$NON-NLS-2$
			List<String> envs = component.getExecutionEnvironments();
			assertEquals(List.of("J2SE-1.4"), envs, "Wrong execution environments"); //$NON-NLS-1$ //$NON-NLS-2$

			IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
			assertEquals(11, requiredComponents.length, "Wrong number of required components"); //$NON-NLS-1$

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
			reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", //$NON-NLS-1$
					new BundleVersionRange("[3.29.0,4.0.0)"))); //$NON-NLS-1$

			for (int i = 0; i < reqs.size(); i++) {
				assertEquals(reqs.get(i), requiredComponents[i], "Wrong required component"); //$NON-NLS-1$
			}
		} finally {
			ApiTestingEnvironment.dispose(baseline);
		}
	}

	@Test
	public void testReExport() throws CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-manifests"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue(file.exists(), "Missing manifest directory"); //$NON-NLS-1$
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test", TestSuiteHelper.getEEDescription(), null); //$NON-NLS-1$
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
			assertTrue(debugCoreExport, "org.eclipse.debug.core should be re-exported"); //$NON-NLS-1$
			assertFalse(others, "Other components should not be re-exported"); //$NON-NLS-1$
		} finally {
			ApiTestingEnvironment.dispose(baseline);
		}
	}
}
