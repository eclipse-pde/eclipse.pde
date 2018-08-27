/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
