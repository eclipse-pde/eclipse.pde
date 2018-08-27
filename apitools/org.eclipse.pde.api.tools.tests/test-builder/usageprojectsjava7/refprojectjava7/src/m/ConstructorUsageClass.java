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
package m;

/**
 * 
 */
public class ConstructorUsageClass {

	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public ConstructorUsageClass() {
	}
	/**
	 * Constructor
	 * @param i
	 * @param o
	 * @param chars
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public ConstructorUsageClass(int i, Object o, char[] chars) {
	}
	public static class inner {
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner() {
		}
		/**
		 * Constructor
		 * @param i
		 * @param o
		 * @param chars
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner(int i, Object o, char[] chars){
			
		}
	}
	
	class inner2 {
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner2() {
		}
		
		/**
		 * Constructor
		 * @param i
		 * @param o
		 * @param chars
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner2(int i, Object o, char[] chars) {
			
		}
	}
}

class outer2 {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public outer2() {
	}
	/**
	 * Constructor
	 * @param i
	 * @param o
	 * @param chars
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public outer2(int i, Object o, char[] chars) {
		
	}
}
