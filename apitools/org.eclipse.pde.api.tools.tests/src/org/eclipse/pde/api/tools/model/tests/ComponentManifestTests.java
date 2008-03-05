/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.BundleVersionRange;
import org.eclipse.pde.api.tools.internal.RequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;

/**
 * @since 1.0.0
 */
public class ComponentManifestTests extends TestCase {

	public static Test suite() {
		return new TestSuite(ComponentManifestTests.class);
	}	
	
	public ComponentManifestTests() {
		super();
	}
	
	public ComponentManifestTests(String name) {
		super(name);
	}
	
	public void testComponentManifest() throws FileNotFoundException, CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-manifests");
		File file = path.toFile();
		assertTrue("Missing manifest directory", file.exists());
		IApiProfile baseline = TestSuiteHelper.newApiProfile("test", TestSuiteHelper.getEEDescriptionFile());
		IApiComponent component = baseline.newApiComponent(file.getAbsolutePath());
		baseline.addApiComponents(new IApiComponent[] { component });
		assertEquals("Id: ", "org.eclipse.debug.ui" , component.getId());
		assertEquals("Name: ", "Debug Platform UI" , component.getName());
		assertEquals("Version: ", "3.3.100.qualifier" , component.getVersion());
		String[] envs = component.getExecutionEnvironments();
		assertEquals("Wrong number of execution environments", 1, envs.length);
		assertEquals("Version: ", "J2SE-1.4" , envs[0]);
		
		IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
		assertEquals("Wrong number of required components", 11, requiredComponents.length);
		
		List<RequiredComponentDescription> reqs = new ArrayList<RequiredComponentDescription>();
		reqs.add(new RequiredComponentDescription("org.eclipse.core.expressions", new BundleVersionRange("(3.3.0,4.0.0)")));
		reqs.add(new RequiredComponentDescription("org.eclipse.core.variables", new BundleVersionRange("[3.2.0,4.0.0]")));
		reqs.add(new RequiredComponentDescription("org.eclipse.ui", new BundleVersionRange("[3.3.0,4.0.0]")));
		reqs.add(new RequiredComponentDescription("org.eclipse.ui.console", new BundleVersionRange("[3.2.0,4.0.0)")));
		reqs.add(new RequiredComponentDescription("org.eclipse.help", new BundleVersionRange("3.3.0")));
		reqs.add(new RequiredComponentDescription("org.eclipse.debug.core", new BundleVersionRange("3.4.0")));
		reqs.add(new RequiredComponentDescription("org.eclipse.jface.text", new BundleVersionRange("[3.3.0,4.0.0)")));
		reqs.add(new RequiredComponentDescription("org.eclipse.ui.workbench.texteditor", new BundleVersionRange("[3.3.0,4.0.0)")));
		reqs.add(new RequiredComponentDescription("org.eclipse.ui.ide", new BundleVersionRange("[3.3.0,4.0.0)")));
		reqs.add(new RequiredComponentDescription("org.eclipse.ui.editors", new BundleVersionRange("[3.3.0,4.0.0)")));
		reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange("[3.3.0,4.0.0)")));
		
		for (int i = 0; i < reqs.size(); i++) {
			assertEquals("Wrong required component", reqs.get(i), requiredComponents[i]);
		}
		baseline.dispose();
	}
}
