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

import m.MR;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests illegal use of method accessed by method reference
 */

public class testMethodReference2 {
public  void m1() {
	  String[] array = {"one"};
	  Arrays.sort(array,   MR::mrCompare);
	  MR mr = new MR();
	  Arrays.sort(array, mr::mrCompare2);
	  MR mr2 = new MR();
	  mr2.con(HashSet<String>::new);
	  }
 }