/*******************************************************************************
 * Copyright (c) April 8, 2013 IBM Corporation and others.
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

/**
 * Tests all of the allowed uses of the @noreference tag
 * on interface fields
 * 
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=402393
 */
public interface test5 {
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public String f1 ="";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	String f2 = "";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	static String f3 = "";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public static String f4 = "";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public final String f5 = "";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	final String f6 = "";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public static final String f7 = "";
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	static final String f8 = "";
}
