/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;
import java.text.Collator;

public class ArraySorter extends Sorter {
	public static ArraySorter INSTANCE = new ArraySorter();
public boolean compare(Object elementOne, Object elementTwo) {
	Collator collator = Collator.getInstance();
	String s1 = elementOne.toString();
	String s2 = elementTwo.toString();
	return collator.compare(s2, s1) > 0;
}
}
