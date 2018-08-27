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
public class testMRL5 {
	public static class inner {
		public static class inner2 {
			public internal[] m1() {
				return null;
			}
			
			protected internal m2() {
				return null;
			}
		}
	}
	
	public static class inner2 {
		public Iinternal[] m3() {
			return null;
		}
		
		protected Iinternal m4() {
			return null;
		}
	}
}
