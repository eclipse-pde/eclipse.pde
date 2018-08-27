/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package x.y.z;

import c.ClassUsageClass;

/**
 * 
 */
public class testA7 {

	public void m1() {
		class inner extends ClassUsageClass {
			
		}
		new inner();
	}
	
	public void m2() {
		class inner extends ClassUsageClass {
			
		}
		new inner();
	}
	
	public void m3() {
		class inner extends ClassUsageClass {
			
		}
		new inner();
	}
}
