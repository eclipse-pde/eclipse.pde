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
 * Test unsupported @noimplement tag on methods in outer / inner annotations
 */
public @interface test5 {
	@interface inner {
		/**
		 * @noimplement
		 * @return
		 */
		public int m1();
		
		/**
		 * @noimplement
		 * @return
		 */
		public abstract char m2();
		@interface inner2 {
			/**
			 * @noimplement
			 * @return
			 */
			public int m1();
			
			/**
			 * @noimplement
			 * @return
			 */
			public abstract char m2();
		}
	}
}

@interface outer {
	/**
	 * @noimplement
	 * @return
	 */
	public int m1();
	
	/**
	 * @noimplement
	 * @return
	 */
	public abstract char m2();
}
