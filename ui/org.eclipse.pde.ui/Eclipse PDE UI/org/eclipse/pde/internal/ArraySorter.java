package org.eclipse.pde.internal;

public class ArraySorter extends Sorter {
	public static ArraySorter INSTANCE = new ArraySorter();
public boolean compare(Object elementOne, Object elementTwo) {
	return elementTwo.toString().compareTo(elementOne.toString()) > 0;
}
}
