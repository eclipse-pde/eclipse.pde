/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Test unsupported @nooverride tag on private methods in an enum in the default package
 */
public enum test9 {
	A;
	/**
	 * @nooverride
	 * @return
	 */
	private int m1() {
		return 0;
	}
	
	/**
	 * @nooverride
	 * @return
	 */
	private final char m2() {
		return 's';
	}
}
