package classes;
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

/**
 * 
 * @since
 */
public class Test10 {
	
	String[] strs = null;
	Integer[][] ints = null;
	
	public void arrayTypes() {
		int[] nums = new int[1];
		strs = new String[1];
		strs[0] = "foo";
		char[][] chars = new char[1][1];
		Object[][] objs = new Object[1][1];
		objs[0][0] = new Object();
		ints = new Integer[1][1];
		Double[][][][][][][][][] dbles = new Double[1][0][0][0][0][0][0][0][0];
	}
	
}