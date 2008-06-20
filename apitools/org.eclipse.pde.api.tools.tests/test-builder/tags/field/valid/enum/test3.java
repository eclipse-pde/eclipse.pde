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
 * Tests the valid use of field tags on an enum in the defualt package
 * 
 * @since 3.4
 */
public enum test3 {
	A,
	B;
	
	public Object f1 = null;
	protected int f2 = 0;
	private char[] f3 = {};
}
