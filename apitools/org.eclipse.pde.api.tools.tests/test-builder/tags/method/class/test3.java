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
 * Test supported @noextend tag on methods in outer / inner classes
 */
public class test3 {
	
	/**
	 * @noextend
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noextend
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noextend
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noextend
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
	
	static class inner {
		/**
		 * @noextend
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noextend
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noextend
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noextend
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noextend
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noextend
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @noextend
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noextend
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
	 * @noextend
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noextend
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noextend
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noextend
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
