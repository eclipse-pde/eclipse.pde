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
 * Test unsupported @nooverride tag on methods in an annotation in the default package
 */
public @interface test8 {
	/**
	 * @nooverride
	 * @return
	 */
	public int m1();
	
	/**
	 * @nooverride
	 * @return
	 */
	public abstract char m2();
}
