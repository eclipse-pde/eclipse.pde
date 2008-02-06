/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * 
 * @since
 */
public class TestField4 {
	static class Inner1 {
		/**
		 * @noreference
		 */
		Object field = null;
		
		class Inner3 {
			/**
			 * @noreference
			 */
			Object field = null;
			
			class Inner {
				/**
				 * @noreference
				 */
				int number = -1;
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
