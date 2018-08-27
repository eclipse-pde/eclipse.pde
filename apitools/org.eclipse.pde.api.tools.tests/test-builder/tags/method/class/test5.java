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
 * Test supported @noinstantiate tag on methods in outer / inner classes
 */
public class test5 {
	/**
	 * @noinstantiate
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noinstantiate
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noinstantiate
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noinstantiate
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
	
	static class inner {
		/**
		 * @noinstantiate
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noinstantiate
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noinstantiate
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noinstantiate
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noinstantiate
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noinstantiate
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @noinstantiate
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noinstantiate
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
	 * @noinstantiate
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noinstantiate
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noinstantiate
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noinstantiate
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
