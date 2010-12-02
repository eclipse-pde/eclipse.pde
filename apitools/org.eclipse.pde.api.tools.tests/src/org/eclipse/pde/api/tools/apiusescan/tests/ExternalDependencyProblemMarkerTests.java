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
package org.eclipse.pde.api.tools.apiusescan.tests;

import junit.framework.TestCase;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;

public class ExternalDependencyProblemMarkerTests extends TestCase {

	private IJavaProject fProject;

	protected void setUp() throws Exception {
		IProject setupProject = ExternalDependencyTestUtils.setupProject();
		if (setupProject == null) {
			fail("Unable to setup the project. Can not run the test cases");
			return;
		}
		fProject = JavaCore.create(setupProject);
		
		String location = ExternalDependencyTestUtils.setupReport("reportAll", true);
		if (location == null) {
			fail("Could not setup the report : reportAll.zip");
		}		
	}
	
	public void testMissingType() {
		try {
			
			IType type = fProject.findType("tests.apiusescan.coretestproject.IConstants");
			type.rename("IConstants1", true, null);
			IProject project = fProject.getProject();
			ExternalDependencyTestUtils.waitForBuild();
			
			IMarker[] markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			assertEquals("No Api Use Scan problem marker found for missing type IConstants", 1, markers.length);
			String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
			assertEquals("Marker for missing type IConstants not found","tests.apiusescan.coretestproject.IConstants", typeName);
			
			type = fProject.findType("tests.apiusescan.coretestproject.IConstants1");
			type.rename("IConstants", true, null);
			ExternalDependencyTestUtils.waitForBuild();		
			markers = project.findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			assertEquals("Api Use Scan problem marker for missing type IConstants did not clear", 0, markers.length);			
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	public void testMissingMethod() {
		try {			
			IType type = fProject.findType("tests.apiusescan.coretestproject.ITestInterface");
			IMethod method = type.getMethods()[0];
			method.rename("performTask1", true, null);
			ExternalDependencyTestUtils.waitForBuild();
			
			IMarker[] markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);			
			assertEquals("No Api Use Scan problem marker found for missing method ITestInterface.performTask()", 1, markers.length);
			
			String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
			assertEquals("Marker for missing method ITestInterface.performTask() not found","tests.apiusescan.coretestproject.ITestInterface", typeName);
			
			type = fProject.findType("tests.apiusescan.coretestproject.ITestInterface");
			method = type.getMethods()[0];
			method.rename("performTask", true, null);
			ExternalDependencyTestUtils.waitForBuild();	
			
			markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			assertEquals("Api Use Scan problem marker for missing method ITestInterface.performTask() did not clear.", 0, markers.length);			
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	public void testMissingField() {
		try {			
			IType type = fProject.findType("tests.apiusescan.coretestproject.TestInterfaceImpl");
			IField field = type.getField("fField");
			field.rename("fField1", true, null);
			ExternalDependencyTestUtils.waitForBuild();
			
			IMarker[] markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);			
			assertEquals("No Api Use Scan problem marker found for missing field TestInterfaceImpl.fField", 1, markers.length);
			
			String typeName = markers[0].getAttribute(IApiMarkerConstants.API_USESCAN_TYPE, null);
			assertEquals("Marker for missing field TestInterfaceImpl.fField not found","tests.apiusescan.coretestproject.TestInterfaceImpl", typeName);
			
			type = fProject.findType("tests.apiusescan.coretestproject.TestInterfaceImpl");
			field = type.getField("fField1");
			field.rename("fField", true, null);
			ExternalDependencyTestUtils.waitForBuild();	
			
			markers = type.getUnderlyingResource().findMarkers(IApiMarkerConstants.API_USESCAN_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			assertEquals("Api Use Scan problem marker for missing field TestInterfaceImpl.fField did not clear.", 0, markers.length);			
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}
}
