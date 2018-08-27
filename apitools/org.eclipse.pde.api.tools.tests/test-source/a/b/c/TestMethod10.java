/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import java.util.List;

/**
 * Tests that methods with many Object parameters are processed correctly
 * @since
 */
public class TestMethod10 {

	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @param name
	 * @param number
	 */
	public void one(String name, Integer number) {
	}
	
	/**
	 * @nooverride
	 * @param name
	 * @param number
	 */
	public void one(String[][] name, Integer number) {
	}
	
	/**
	 * @nooverride
	 * @param list
	 * @param runnable
	 */
	protected void two(List list, Runnable runnable) {
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride
	 * @param name
	 * @param number
	 */
	public void one(Object name, Integer number) {
	}
}
