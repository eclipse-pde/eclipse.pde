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
package x.y.z;

import c.ClassUsageClass;

public class testA2 {

	public Object m1() {
		return new ClassUsageClass() {
			public String toString() {
				return super.toString();
			}
		};
	}
}
