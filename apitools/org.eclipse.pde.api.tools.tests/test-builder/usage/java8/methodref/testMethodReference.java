/*******************************************************************************
 * Copyright (c) April 5, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/


package x.y.z;

import m.MethodReference;
import m.MethodReferenceInterface;
import m.ConstructorReference;
import m.ConstructorReferenceInterface;
import m.ConstructorReferenceInterfaceArg;
import m.ConstructorReferenceInterfaceParamArg;
import java.util.ArrayList;



/**
 * Tests illegal use of method accessed by method reference
 */
public class testMethodReference {

	public void m1(){
		 MethodReferenceInterface met = MethodReference::method1;
		 met.process();	
		 MethodReference m = new MethodReference();
		 met = m::method2;
		 met.process();
		 ConstructorReferenceInterface<ConstructorReference> con = ConstructorReference::new;
		 con.create1().getString();
		 ConstructorReferenceInterfaceArg<ConstructorReference,String> conWithArg = ConstructorReference::new;     
		 conWithArg.create2("test").getString();
		 ConstructorReferenceInterfaceParamArg<ConstructorReference,String> conParamArg = ConstructorReference::<String>new;
		 conParamArg.create3(new ArrayList<String>()).getStrings().size();
	}
		
}
