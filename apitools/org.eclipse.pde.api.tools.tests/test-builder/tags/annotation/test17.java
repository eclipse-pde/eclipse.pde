/*******************************************************************************
 * Copyright (c) Aug 20, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *
 */
public class test17 {

	/**
	 * @noreference
	 */
	private @interface inner {
		
	}
	
	public static class C1 {
		/**
		 * @noreference This annotation is not intended to be referenced by clients.
		 */
		@interface A1 {
			class C2 {
				/**
				 * @noreference
				 */
				@interface A2 {
					
				}
			}
		}
	}
}
