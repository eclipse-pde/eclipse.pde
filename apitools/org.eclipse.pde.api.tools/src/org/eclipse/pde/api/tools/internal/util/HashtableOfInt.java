/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import org.eclipse.core.runtime.Assert;
 
 /**
  *	Hashtable for non-zero int keys.
  *
  * All entries into the table must be greater than zero.
  */
public final class HashtableOfInt {
	
	// to avoid using Enumerations, walk the individual tables skipping nulls
	public int[] keyTable;
	public Object[] valueTable;
	public int elementSize; // number of elements in the table
	int threshold;
	
	/**
	 * Constructor
	 */
	public HashtableOfInt() {
		this(5);
	}
	
	/**
	 * Constructor
	 * @param size
	 */
	public HashtableOfInt(int size) {
		this.elementSize = 0;
		this.threshold = size; // size represents the expected number of elements
		int extraRoom = (int) (size * 1.75f);
		if (this.threshold == extraRoom)
			extraRoom++;
		this.keyTable = new int[extraRoom];
		this.valueTable = new Object[extraRoom];
	}
	
	/**
	 * Returns if the table contains the given key
	 * @param key
	 * @return if the table contains the given key
	 */
	public boolean containsKey(int key) {
		int length = keyTable.length, index = key % length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key)
				return true;
			if (++index == length) {
				index = 0;
			}
		}
		return false;
	}
	
	/**
	 * Returns the object mapped to the given key or <code>null</code> if there is no mapping
	 * for the given key
	 * @param key
	 * @return the object mapped to the given key or <code>null</code>
	 */
	public Object get(int key) {
		int length = keyTable.length, index = key % length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key)  return valueTable[index];
			if (++index == length) {
				index = 0;
			}
		}
		return null;
	}
	
	/**
	 * Maps the given key to the specified value
	 * @param key
	 * @param value
	 * @return the value from an existing mapping (if there was one)
	 */
	public Object put(int key, Object value) {
		Assert.isLegal(key > 0, "Only keys greater than zero can be used"); //$NON-NLS-1$
		int length = keyTable.length, index = key % length;
		int currentKey;
		while ((currentKey = keyTable[index]) != 0) {
			if (currentKey == key)  return valueTable[index] = value;
			if (++index == length) {
				index = 0;
			}
		}
		keyTable[index] = key;
		valueTable[index] = value;
	
		// assumes the threshold is never equal to the size of the table
		if (++elementSize > threshold)
			rehash();
		return value;
	}
	
	/**
	 * Re-hashes on a collision
	 */
	private void rehash() {
		HashtableOfInt newHashtable = new HashtableOfInt(elementSize * 2); // double the number of expected elements
		int currentKey;
		for (int i = keyTable.length; --i >= 0;)
			if ((currentKey = keyTable[i]) != 0)
				newHashtable.put(currentKey, valueTable[i]);
	
		this.keyTable = newHashtable.keyTable;
		this.valueTable = newHashtable.valueTable;
		this.threshold = newHashtable.threshold;
	}
	
	/**
	 * Returns the current size of the table
	 * @return the size of the table
	 */
	public int size() {
		return elementSize;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = ""; //$NON-NLS-1$
		Object object;
		for (int i = 0, length = valueTable.length; i < length; i++)
			if ((object = valueTable[i]) != null)
				s += keyTable[i] + " -> " + object.toString() + "\n"; //$NON-NLS-2$ //$NON-NLS-1$
		return s;
	}
}
