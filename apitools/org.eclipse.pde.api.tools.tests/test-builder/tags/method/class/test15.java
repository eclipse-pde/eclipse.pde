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
 * Test unsupported @noreference tag on private constructors in outer / inner classes
 */
public class test15 {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private test15() {
		
	}
	
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private test15(int i) {
		
	}
	static class inner {
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		private inner() {
			
		}
		
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		private inner(int i) {
			
		}
		class inner2 {
			/**
			 * Constructor
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			private inner2() {
				
			}
			
			/**
			 * Constructor
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			private inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private outer() {
		
	}
	
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private outer(int i) {
		
	}
}
