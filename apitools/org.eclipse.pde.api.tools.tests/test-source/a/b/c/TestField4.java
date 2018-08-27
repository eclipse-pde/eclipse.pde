/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
 * 
 * @since
 */
public class TestField4 {
	public static class Inner1 {
		/**
		 * @noreference
		 */
		public Object field = null;
		
		protected class Inner3 {
			/**
			 * @noreference
			 */
			protected Object field = null;
			
			public class Inner {
				/**
				 * @noreference
				 */
				protected int number = -1;
			}
		}
	}
	
	class Inner2 {
		/**
		 * @noreference
		 */
		Object field = null;
	}
}
