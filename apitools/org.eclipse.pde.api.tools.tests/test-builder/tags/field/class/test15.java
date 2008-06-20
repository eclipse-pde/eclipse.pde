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

/**
 * Test unsupported @noextend tag on fields in a class in the default package
 */
public class test15 {
	/**
	 * @noextend
	 */
	public Object f1 = null;
	/**
	 * @noextend
	 */
	protected int f2 = 0;
	/**
	 * @noextend
	 */
	private static char[] f3 = {};
}
