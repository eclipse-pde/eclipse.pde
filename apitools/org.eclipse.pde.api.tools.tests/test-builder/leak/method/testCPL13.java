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
package x.y.z;

import internal.x.y.z.Iinternal;
import internal.x.y.z.internal;

/**
 * 
 */
public class testCPL13 {
	public static class inner {
		public static class inner2 {
			/**
			 * Constructor
			 * @param i
			 * @param ii
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			public inner2(internal i, Iinternal ii) {
				
			}
			
			/**
			 * Constructor
			 * @param i
			 * @param o
			 * @param ii
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			public inner2(internal i, internal i2, Iinternal ii) {
				
			}
		}
	}
	
	public static class inner3 {
		/**
		 * Constructor
		 * @param s
		 * @param i
		 * @param n
		 * @param ii
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner3(String s, internal i, int n, Iinternal ii) {
			
		}
		
		/**
		 * Constructor
		 * @param s
		 * @param i
		 * @param n
		 * @param i2
		 * @param ii
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner3(String s, internal i, int n, internal i2, Iinternal ii) {
			
		}
	}
}
