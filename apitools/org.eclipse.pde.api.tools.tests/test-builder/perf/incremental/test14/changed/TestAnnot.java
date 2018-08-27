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

/**
 * Test annotation for Java 5 performance testing
 * 
 * @since 1.0.0
 * @noinstantiate
 */
public @interface TestAnnot {

	/**
	 * 
	 */
	public String name = null;
	
	/**
	 * @return value
	 * @nooverride
	 */
	public int m1() default -1;
}
