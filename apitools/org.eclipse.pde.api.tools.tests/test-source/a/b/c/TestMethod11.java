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
	 * @noextend
	 * @param nums
	 * @param vals
	 */
	protected void two(float[][] nums, double[] vals) {
		
	}
	
}
