package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public class ArraySorter extends Sorter {
	public static ArraySorter INSTANCE = new ArraySorter();
public boolean compare(Object elementOne, Object elementTwo) {
	return elementTwo.toString().compareTo(elementOne.toString()) > 0;
}
}
