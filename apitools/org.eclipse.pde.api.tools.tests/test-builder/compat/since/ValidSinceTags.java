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
package a.since;

/**
 *
 */
public class ValidSinceTags {
	/**
	 * @since a 1.0
	 */
	public void m1() {}
	
	/**
	 * @since 1.0 a
	 */
	public void m2() {}
	
	/**
	 * @since a 1.0 b
	 */
	public void m3() {}	
	
	/**
	 * @since 1.0,a
	 */
	public void m4() {}		
	
	/**
	 * @since a,1.0,b
	 */
	public void m5() {}	
	
	/**
	 * @since 1.0 , was added in 3.1 as private method
	 */
	public void m6() {}
	
	/**
	 * @since 1.0 protected (was added in 2.1 as private class)
	 */
	public void m7() {}
}
