/*******************************************************************************
 * Copyright (c) April 15, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/


package x.y.z;


import m.ConstructorReference2;
import m.ConstructorReferenceInterface;
import m.ConstructorReferenceInterfaceArg;
import m.ConstructorReferenceInterfaceParamArg;
import java.util.ArrayList;



/**
 * Tests illegal use of constructor reference if no instantiate on class
 */
public class testConstructorRefInstantiate {

	public void m1(){
		 ConstructorReferenceInterface<ConstructorReference2> con = ConstructorReference2::new;
		 con.create1().getString();
		 ConstructorReferenceInterfaceArg<ConstructorReference2,String> conWithArg = ConstructorReference2::new;     
		 conWithArg.create2("test").getString();
		 ConstructorReferenceInterfaceParamArg<ConstructorReference2,String> conParamArg = ConstructorReference2::<String>new;
		 conParamArg.create3(new ArrayList<String>()).getStrings().size();
	}
		
}
