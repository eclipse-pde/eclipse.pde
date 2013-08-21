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
public class test15 {

	/**
	 * @noreference This annotation is not intended to be referenced by clients.
	 */
	private @interface inner1 {
		
		/**
		 * @noreference
		 */
		@interface inner2 {
			
		}
	}
}
