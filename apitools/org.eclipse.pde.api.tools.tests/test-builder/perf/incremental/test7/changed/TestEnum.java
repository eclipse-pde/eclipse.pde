/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package api;

import internal.InternalClass;

import org.eclipse.debug.core.DebugPlugin;


/**
 * Test enum for Java 5 performance testing
 * 
 * @since 1.0.0
 * @nooverride
 */
public enum TestEnum {

	/**
	 * 
	 */
	A, 
	/*B, */
	C;
	
	/**
	 * 
	 */
	public void m1() {
		DebugPlugin dp = new DebugPlugin();
		System.out.println("m1"); //$NON-NLS-1$
	}
	/**
	 * @since 1.0
	 */
	public InternalClass m2() {
		return null;
	}
}
