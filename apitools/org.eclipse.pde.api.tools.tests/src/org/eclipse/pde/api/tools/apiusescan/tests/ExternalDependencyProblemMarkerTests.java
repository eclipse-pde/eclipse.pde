/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.apiusescan.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.junit.Before;
import org.junit.Test;

public class ExternalDependencyProblemMarkerTests {

	private IJavaProject fProject;

	@Before
	public void setUp() throws Exception {
		IProject setupProject = ExternalDependencyTestUtils.setupProject();
		if (setupProject == null) {
			fail("Unable to setup the project. Can not run the test cases"); //$NON-NLS-1$
			return;
		}
		fProject = JavaCore.create(setupProject);

		String location = ExternalDependencyTestUtils.setupReport("reportAll", true); //$NON-NLS-1$
		if (location == null) {
			fail("Could not setup the report : reportAll.zip"); //$NON-NLS-1$
		}
	}

	@Test
	public void testMissingType() throws CoreException {
		IType type = fProject.findType("tests.apiusescan.coretestproject.IConstants"); //$NON-NLS-1$
		type.rename("IConstants1", true, null); //$NON-NLS-1$
		IProject project = fProject.getProject();
		ExternalDependencyTestUtils.waitForBuild();

		IMarker[] markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false,
				IResource.DEPTH_ZERO);
		assertEquals("No API Use Scan problem marker found for missing type IConstants", 1, markers.length); //$NON-NLS-1$
		String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
		assertEquals("Marker for missing type IConstants not found", "tests.apiusescan.coretestproject.IConstants", //$NON-NLS-1$ //$NON-NLS-2$
				typeName);

		type = fProject.findType("tests.apiusescan.coretestproject.IConstants1"); //$NON-NLS-1$
		type.rename("IConstants", true, null); //$NON-NLS-1$
		ExternalDependencyTestUtils.waitForBuild();
		markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
		assertEquals("API Use Scan problem marker for missing type IConstants did not clear", 0, markers.length); //$NON-NLS-1$
	}

	@Test
	public void testMissingMethod() throws CoreException {
		IType type = fProject.findType("tests.apiusescan.coretestproject.ITestInterface"); //$NON-NLS-1$
		IMethod method = type.getMethods()[0];
		method.rename("performTask1", true, null); //$NON-NLS-1$
		ExternalDependencyTestUtils.waitForBuild();

		IMarker[] markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER,
				false, IResource.DEPTH_ZERO);
		assertEquals("No API Use Scan problem marker found for missing method ITestInterface.performTask()", 1, //$NON-NLS-1$
				markers.length);

		String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
		assertEquals("Marker for missing method ITestInterface.performTask() not found", //$NON-NLS-1$
				"tests.apiusescan.coretestproject.ITestInterface", typeName); //$NON-NLS-1$

		type = fProject.findType("tests.apiusescan.coretestproject.ITestInterface"); //$NON-NLS-1$
		method = type.getMethods()[0];
		method.rename("performTask", true, null); //$NON-NLS-1$
		ExternalDependencyTestUtils.waitForBuild();

		markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false,
				IResource.DEPTH_ZERO);
		assertEquals("API Use Scan problem marker for missing method ITestInterface.performTask() did not clear.", 0, //$NON-NLS-1$
				markers.length);
	}

	@Test
	public void testMissingField() throws CoreException {
		IType type = fProject.findType("tests.apiusescan.coretestproject.TestInterfaceImpl"); //$NON-NLS-1$
		IField field = type.getField("fField"); //$NON-NLS-1$
		field.rename("fField1", true, null); //$NON-NLS-1$
		ExternalDependencyTestUtils.waitForBuild();

		IMarker[] markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER,
				false, IResource.DEPTH_ZERO);
		assertEquals("No API Use Scan problem marker found for missing field TestInterfaceImpl.fField", 1, //$NON-NLS-1$
				markers.length);

		String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
		assertEquals("Marker for missing field TestInterfaceImpl.fField not found", //$NON-NLS-1$
				"tests.apiusescan.coretestproject.TestInterfaceImpl", typeName); //$NON-NLS-1$

		type = fProject.findType("tests.apiusescan.coretestproject.TestInterfaceImpl"); //$NON-NLS-1$
		field = type.getField("fField1"); //$NON-NLS-1$
		field.rename("fField", true, null); //$NON-NLS-1$
		ExternalDependencyTestUtils.waitForBuild();

		markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false,
				IResource.DEPTH_ZERO);
		assertEquals("API Use Scan problem marker for missing field TestInterfaceImpl.fField did not clear.", 0, //$NON-NLS-1$
				markers.length);
	}

	@Test
	public void testMissingInnerType() throws CoreException {
		IType type = fProject.findType("tests.apiusescan.coretestproject.ClassWithInnerType.InnerType"); //$NON-NLS-1$
		type.rename("InnerType1", true, null); //$NON-NLS-1$
		IProject project = fProject.getProject();
		ExternalDependencyTestUtils.waitForBuild();

		IMarker[] markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false,
				IResource.DEPTH_ZERO);
		assertEquals("No API Use Scan problem marker found for missing type IConstants", 1, markers.length); //$NON-NLS-1$
		String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
		assertEquals("Marker for missing type InnerType not found", //$NON-NLS-1$
				"tests.apiusescan.coretestproject.ClassWithInnerType.InnerType", typeName); //$NON-NLS-1$

		type = fProject.findType("tests.apiusescan.coretestproject.ClassWithInnerType.InnerType1"); //$NON-NLS-1$
		type.rename("InnerType", true, null); //$NON-NLS-1$
		ExternalDependencyTestUtils.waitForBuild();
		markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
		assertEquals("API Use Scan problem marker for missing type InnerType did not clear", 0, markers.length); //$NON-NLS-1$
	}
}
