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
public class testMPL9 {

	public static class inner {
		public static class inner2 {
			public void m1(Iinternal i, Object o, double d, internal ii) {
				
			}
			
			public Object m2(Iinternal i, Object o, double d, internal ii) {
				return null;
			}
			
			public char[] m3(Iinternal i, Object o, double d, internal ii) {
				return new char[0];
			}
		}
	}
	
	public static class inner3 {
		protected void m4(Iinternal i, Object o, double d, internal ii) {
			
		}
		
		protected Object m5(Iinternal i, Object o, double d, internal ii) {
			return null;
		}
		
		protected char[] m6(Iinternal i, Object o, double d, internal ii) {
			return new char[0];	
		}
	}
}
