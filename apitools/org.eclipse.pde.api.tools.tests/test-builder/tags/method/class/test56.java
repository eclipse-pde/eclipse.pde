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
package a.b.c;

/**
 * Test unsupported @noreference tag on private class constructors
 */
public class test56 {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 * @noimplement
	 * @noinstantiate
	 */
	private test56() {
		
	}
	
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 * @nooverride
	 * @noextend
	 */
	private test56(int i) {
		
	}
}
