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
public class testCPL10 {
	public static class inner {
		public static class inner2 {
			private inner2(internal i, Iinternal ii) {
				
			}
			
			private inner2(internal i, Object o, Iinternal ii) {
				
			}
		}
	}
	
	public static class inner3 {
		private inner3(String s, internal i, int n, Iinternal ii) {
			
		}
		
		private inner3(String s, internal i, int n, double d, Iinternal ii) {
			
		}
	}
}
