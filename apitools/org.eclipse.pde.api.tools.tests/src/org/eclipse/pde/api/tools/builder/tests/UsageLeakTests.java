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
package org.eclipse.pde.api.tools.builder.tests;

/**
 * Tests usage scanning with and without noreference tags in source
 * @since 1.0.0 
 */
public class UsageLeakTests extends ApiBuilderTests {

	/**
	 * Constructor
	 */
	public UsageLeakTests() {
		super("API usage leak tests");
	}
	
	/**
	 * Tests that a field leaking an internal type
	 * via its' type is flagged correctly as a leak
	 */
	public void testLeaksInternalFieldType() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that if a field leaks an internal type via 
	 * its type, that the problem is ignored when there
	 * is an @noreference tag on the field
	 */
	public void testIgnoresNoRefInternalFieldType() {
		//TODO provide code for tests
	}

	/**
	 * Tests that a method leaking an internal type via its' return
	 * type is flagged properly as a leak
	 */
	public void testLeaksMethodInternalReturnType() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking an internal type via its'
	 * return type is ignored as a problem if the method 
	 * has an @noreference tag on it
	 */
	public void testIgnoreNoRefMethodInternalReturnType() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking an internal type via a parameter
	 * is flagged properly as a leak
	 */
	public void testLeaksMethodParameterInternalType() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method that leaks an internal type via one
	 * of its' parameters is ignored as a problem if there is an
	 * @noreference tag on the method
	 */
	public void testIgnoreNoRefMethodParameterInternalType() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking internal types via parameters
	 * is flagged properly as a leak.
	 */
	public void testLeaksMethodParametersInternalTypes() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking internal types via parameters
	 * is ignored as a problem if there is an @noreference tag
	 * on the method
	 */
	public void testIgnoreNoRefMethodParametersInternalTypes() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking an internal type via the parameter on
	 * a generic is flagged properly as a leak
	 */
	public void testLeaksMethodReturnGeneric() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking an internal type via 
	 * a parameter in a generic is ignored if there
	 * is an @noreference tag on the method
	 */
	public void testIgnoreNoRefMethodreturnGeneric() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking an internal type via a parameter
	 * of a generic that is a method parameter is flagged properly
	 * as a problem
	 */
	public void testLeaksMethodParameterGeneric() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking an internal type via a generic parameter
	 * in a method parameter is ignored if there is an @noreference tag
	 * on the method
	 */
	public void testIgnoreNoRefethodParameterGeneric() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking internal types via generic parameters
	 * as method parameters is flagged properly as a problem
	 */
	public void testLeaksMethodParametersGenerics() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a method leaking internal types via generic parameters
	 * as method parameters is ignored if there is an @noreference tag on the 
	 * method
	 */
	public void testIgnoreNoRefMethodParamatersGenerics() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a type that leaks an internal type via an extends
	 * is flagged properly as a problem
	 */
	public void testLeaksClassExtendsInternalType() {
		//TODO provide code for tests
	}
	
	/**
	 * Tests that a type that leaks an internal type via an implements
	 * is flagged properly as a problem
	 */
	public void testLeakInterfaceImplementsInternalType() {
		//TODO provide code for tests
	}
}
