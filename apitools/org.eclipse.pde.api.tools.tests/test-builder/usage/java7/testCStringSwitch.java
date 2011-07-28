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

import c.ClassUsageClass;

public class testCStringSwitch {
	public void m1(){
		ClassUsageClass c = null;
		String testSwitch = "accept";
		switch (testSwitch) {
		case "accept":
			c = new ClassUsageClass();
			break;
		case "reject":
			c = new ClassUsageClass();
			break;
		default:
			c = new ClassUsageClass();
			break;
		}
		c.toString();
	}
}


