/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * Tests that a method with more than one primitive parameter is processed correctly
 * @since
 */
public class TestMethod9 {

	/**
	 * @noreference
	 * @param number
	 * @param dbl
	 * @param flot
	 */
	public void one(int number, double dbl, float flot) {
		
	}
	
	/**
	 * @nooverride
	 * @param number
	 * @param dbl
	 * @param flot
	 */
	protected void two(double dbl, float flot) {
		
	}
	
}
