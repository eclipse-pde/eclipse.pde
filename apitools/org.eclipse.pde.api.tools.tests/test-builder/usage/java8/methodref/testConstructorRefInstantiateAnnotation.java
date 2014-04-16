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


import m.ConstructorReferenceAnno2;
import m.ConstructorReferenceInterface;
import m.ConstructorReferenceInterfaceArg;
import m.ConstructorReferenceInterfaceParamArg;
import java.util.ArrayList;



/**
 * Tests illegal use of constructor reference if no instantiate annotation on class
 */
public class testConstructorRefInstantiateAnnotation {

	public void m1(){
		 ConstructorReferenceInterface<ConstructorReferenceAnno2> con = ConstructorReferenceAnno2::new;
		 con.create1().getString();
		 ConstructorReferenceInterfaceArg<ConstructorReferenceAnno2,String> conWithArg = ConstructorReferenceAnno2::new;     
		 conWithArg.create2("test").getString();
		 ConstructorReferenceInterfaceParamArg<ConstructorReferenceAnno2,String> conParamArg = ConstructorReferenceAnno2::<String>new;
		 conParamArg.create3(new ArrayList<String>()).getStrings().size();
	}
		
}
