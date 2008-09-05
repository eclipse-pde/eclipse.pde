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
}
