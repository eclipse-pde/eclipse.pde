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
public class MethodUsageClass {

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public void m2() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m3() {
		
	}
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static void m4() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public static void m5() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static void m6() {
		
	}
	
	public static class inner {
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 */
		public void m2() {
			
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m3() {
			
		}
		
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public static void m4() {
			
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 */
		public static void m5() {
			
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public static void m6() {
			
		}
	}
	
	class inner2 {
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 */
		public void m2() {
			
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m3() {
			
		}
	}
}

class outer {
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public void m2() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m3() {
		
	}
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static void m4() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public static void m5() {
		
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static void m6() {
		
	}
}
