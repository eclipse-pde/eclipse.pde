package org.eclipse.pde.internal.ui.util;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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
