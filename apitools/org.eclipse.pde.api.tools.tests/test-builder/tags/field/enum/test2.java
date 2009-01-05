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
 * Test unsupported @noreference tag on final fields in an enum in the default package
 */
public enum test2 {
	
	A;
	
	/**
	 * @noreference
	 */
	public final Object f1 = null;
	/**
	 * @noreference
	 */
	protected final int f2 = 0;
	/**
	 * @noreference
	 */
	private final char[] f3 = {};
}
