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
 * Test unsupported @noimplement tag on fields in a enum in the default package
 */
public enum test20 {
	
	A;
	
	/**
	 * @noimplement
	 */
	public Object f1 = null;
	/**
	 * @noimplement
	 */
	protected int f2 = 0;
	/**
	 * @noimplement
	 */
	private static char[] f3 = {};
}
