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
package api;


/**
 * Test enum for Java 5 performance testing
 * 
 * @since 1.0.0
 * @nooverride
 */
public enum TestEnum {

	/**
	 * @noinstantiate
	 */
	A, 
	B, 
	C;
	
	/**
	 * 
	 */
	public void m1() {
		System.out.println("m1"); //$NON-NLS-1$
	}
}
