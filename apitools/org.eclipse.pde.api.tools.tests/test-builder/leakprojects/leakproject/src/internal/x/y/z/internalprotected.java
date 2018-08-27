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
package internal.x.y.z;

/**
 * An internal class with only protected members visible
 */
public class internalprotected {
	
	/**
	 * This is only a leak if someone can extend a class
	 */
	protected void protectedMethod() {
	}

}
