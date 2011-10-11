/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
