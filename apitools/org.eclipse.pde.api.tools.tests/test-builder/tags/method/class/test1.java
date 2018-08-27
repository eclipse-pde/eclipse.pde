/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * Test supported @noimplement tag on methods in outer / inner classes
 */
public class test1 {
	
	/**
	 * @noimplement
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noimplement
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
	
	static class inner {
		/**
		 * @noimplement
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noimplement
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noimplement
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noimplement
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @noimplement
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noimplement
			 * @return
			 */
			protected static Object m4() {
				return null;
			}
		}
	}
}

class outer {
	/**
	 * @noimplement
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noimplement
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
