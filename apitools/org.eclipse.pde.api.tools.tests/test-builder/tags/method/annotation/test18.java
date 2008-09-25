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
package a.b.c;

/**
 * Test unsupported @nooverride tag on methods in an inner annotation
 */
public @interface test18 {
	@interface inner {
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
}
