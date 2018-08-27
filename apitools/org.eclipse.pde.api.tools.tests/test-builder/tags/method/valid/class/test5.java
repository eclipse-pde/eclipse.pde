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
package a.b.c;

/**
 * Test supported @noreference tag on class methods in the default package
 */
public class test5 {
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
