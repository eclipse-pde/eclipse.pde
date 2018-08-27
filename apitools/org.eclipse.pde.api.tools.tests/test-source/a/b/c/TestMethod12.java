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
