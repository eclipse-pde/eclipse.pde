/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;


/**
 * Test supported @nooverride tag on constructors in outer / inner classes
 */
public class test21 {
	
	/**
	 * Constructor
	 * @nooverride This constructor is not intended to be referenced by clients.
	 */
	public test21() {
		
	}
	
	/**
	 * Constructor
	 * @nooverride This constructor is not intended to be referenced by clients.
	 */
	public test21(int i) {
		
	}
	
	static class inner {
		/**
		 * Constructor
		 * @nooverride This constructor is not intended to be referenced by clients.
		 */
		public inner() {
			
		}
		
		/**
		 * Constructor
		 * @nooverride This constructor is not intended to be referenced by clients.
		 */
		protected inner(int i) {
			
		}
		static class inner2 {
			/**
			 * Constructor
			 * @nooverride This constructor is not intended to be referenced by clients.
			 */
			public inner2() {
				
			}
			
			/**
			 * Constructor
			 * @nooverride This constructor is not intended to be referenced by clients.
			 */
			protected inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @nooverride This constructor is not intended to be referenced by clients.
	 */
	public outer() {
		
	}
	
	/**
	 * Constructor
	 * @nooverride This constructor is not intended to be referenced by clients.
	 */
	protected outer(int i) {
		
	}
}
