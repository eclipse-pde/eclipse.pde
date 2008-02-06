package classes;
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