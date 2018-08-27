/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import m.MethodUsageClass;

public class testMStringSwitch {

	public void m1(){
		MethodUsageClass c = new MethodUsageClass();
		String testSwitch = "accept";
		switch (testSwitch) {
		case "accept":
			c.m1();
			c.m3();
			break;
		case "reject":
			c.m1();
			c.m3();
			break;
		default:
			c.m1();
			c.m3();
			break;
		}
	}
		
}
