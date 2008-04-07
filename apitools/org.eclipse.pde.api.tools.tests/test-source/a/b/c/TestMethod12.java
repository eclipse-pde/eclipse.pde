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

import java.util.List;

/**
 * Tests that methods with Object array parameters are processed correctly
 * @since
 */
public class TestMethod12 {

	/**
	 * @noreference
	 * @param names
	 * @param nums
	 */
	public void one(String[] names, Double[][] nums) {
		
	}
	
	/**
	 * @nooverride
	 * @param names
	 * @param runs
	 */
	protected void two(List[][] names, Runnable[] runs) {
		
	}
	
}
