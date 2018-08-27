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
public class testCPL8 {

	public static class inner {
		private inner(internal i, Iinternal ii) {
			
		}
		
		private inner(internal i, Object o, Iinternal ii) {
			
		}
		
		private inner(String s, internal i, int n, Iinternal ii) {
			
		}
		
		private inner(String s, internal i, int n, double d, Iinternal ii) {
			
		}
	}
}
