/*******************************************************************************
 * Copyright (c)  2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

public class CatchAllValue {
	public static CatchAllValue singleton = new CatchAllValue("*"); //$NON-NLS-1$

	public CatchAllValue(String s) {
		//do nothing
	}

	public boolean equals(Object obj) {
		return true;
	}

	public int hashCode() {
		return 31;
	}
}