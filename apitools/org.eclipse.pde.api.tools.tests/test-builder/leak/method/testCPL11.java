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
package x.y.z;

import internal.x.y.z.Iinternal;
import internal.x.y.z.internal;

/**
 * 
 */
public class testCPL11 {
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
		
		public inner3(String s, internal i, int n, internal i2, Iinternal ii) {
			
		}
	}
}
