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
 * Tests that methods with primitive array type parameters are processed correctly
 * @since
 */
public class TestMethod11 {

	/**
	 * @noreference
	 * @param nums
	 * @param names
	 */
	public void one(int[] nums, char[][] names) {
		
	}
	
	/**
	 * @nooverride
	 * @param nums
	 * @param vals
	 */
	protected void two(float[][] nums, double[] vals) {
		
	}
	
}
