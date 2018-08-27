/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
