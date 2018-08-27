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
public class testCPL9 {
	public static class inner {
		public static class inner2 {
			public inner2(internal i, Iinternal ii) {
				
			}
			
			protected inner2(internal i, Object o, Iinternal ii) {
				
			}
		}
	}
	
	public static class inner3 {
		public inner3(String s, internal i, int n, Iinternal ii) {
			
		}
		
		protected inner3(String s, internal i, int n, double d, Iinternal ii) {
			
		}
	}
}
