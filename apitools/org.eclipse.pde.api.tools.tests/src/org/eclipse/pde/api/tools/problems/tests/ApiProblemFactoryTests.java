/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.problems.tests;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

import com.ibm.icu.text.MessageFormat;

/**
 * Tests various aspects of the {@link ApiProblemFactory}
 * 
 * @since 1.0.0
 */
public class ApiProblemFactoryTests extends AbstractApiTest {

	String fDefaulMessage = MessageFormat.format("Message not found for problem id:", new String[0]);
	
	/**
	 * Tests that creating an {@link IApiProblem} does not fail
	 */
	public void testCreateProblem() {
		
	}
	
	public void tesCreateUsageProblem() {
		
	}
	
	public void testCreateSincetagProblem() {
		
	}
	
	public void testCreateVersionProblem() {
		
	}
	
	public void testCreateBinaryProblem() {
		
	}
	
	public void testGetKindFromPref() {
		
	}
	
	public void testGetVersionMessages() {
		
	}
	
	public void testGetUsageMessages() {
		
	}
	
	public void testGetBinaryMessages() {
		
	}
	
	public void testGetSinceTagMessages() {
		
	}
	
	public void testGetKindFromBinaryPref() {
		
	}
	
	public void testGetKindFromUsagePref() {
		
	}
	
	public void testGetKindFromVersionPref() {
		
	}
	
	public void testGetKindFromSincePref() {
		
	}
}
