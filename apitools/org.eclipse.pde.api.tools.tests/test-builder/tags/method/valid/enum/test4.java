/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 * Test supported @noreference tag on methods in outer / inner enums
 */
public enum test4 {
	A;
	public enum inner {
		A;
		/**
		 * @noreference
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noreference
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		public enum inner2 {
			A;
			/**
			 * @noreference
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noreference
			 * @return
			 */
			public final char m2() {
				return 's';
			}
		}
	}
}