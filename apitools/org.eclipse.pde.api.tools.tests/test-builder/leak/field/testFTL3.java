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
public class testFTL3 {
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public internal f1 = null;
	
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public Iinternal[] f2 = null;
	
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	protected internal f3 = null;
	
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 */
	protected Iinternal f4 = null;
	
	public static class inner {
		/**
		 * @noreference This field is not intended to be referenced by clients.
		 */
		public internal f1 = null;
		
		/**
		 * @noreference This field is not intended to be referenced by clients.
		 */
		public Iinternal[] f2 = null;
		
		/**
		 * @noreference This field is not intended to be referenced by clients.
		 */
		protected internal f3 = null;
		
		/**
		 * @noreference This field is not intended to be referenced by clients.
		 */
		protected Iinternal f4 = null;
	}
	
}