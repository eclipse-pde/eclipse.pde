/*******************************************************************************
 * Copyright (c) April 5, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/



package x.y.z;

import m.MRAnnotation;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests illegal use of method accessed by method reference
 */

public class testMethodReferenceAnnotation2 {
public  void m1() {
	  String[] array = {"one"};
	  Arrays.sort(array,   MRAnnotation::mrCompare);
	  MRAnnotation mr = new MRAnnotation();
	  Arrays.sort(array, mr::mrCompare2);
	  MRAnnotation mr2 = new MRAnnotation();
	  mr2.con(HashSet<String>::new);
	  }
 }