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
 * Tests that methods with an Object return type are processed correctly
 * @since
 */
public class TestMethod14 {

	/**
	 * @noreference
	 * @param num
	 * @param dbls
	 * @param cs
	 * @param in
	 */
	public String one(int num, Double[][] dbls, char[] cs, Integer in) {
		return null;
	}
	
	/**
	 * @nooverride
	 * @param ls
	 * @param d
	 * @param c
	 * @param is
	 * @param r
	 */
	protected Double two(List[][] ls, double d, char c, int[] is, Runnable[] r) {
		return null;
	}
	
}
