/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import org.eclipse.pde.api.tools.internal.util.TarEntry;

import junit.framework.TestCase;

/**
 * Test The {@link org.eclipse.pde.api.tools.internal.util.TarEntry} class
 * 
 * @since 1.0.1
 */
public class TarEntryTests extends TestCase {
	
	static final String TAR_NAME = "tar_file";
	
	/**
	 * Tests the constructor
	 */
	public void testConstructors() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals("mode should be 0644", 0644, entry.getMode());
		assertEquals("name sould be 'foo'", TAR_NAME, entry.getName());
	}
	
	/**
	 * Tests the {@link TarEntry#clone()} method
	 */
	public void testClone() {
		TarEntry entry = new TarEntry(TAR_NAME);
		TarEntry entry2 = (TarEntry) entry.clone();
		assertNotNull("The object should have been cloned", entry2);
		assertEquals("the file type should be the same in the cloned entry", entry.getFileType(), entry2.getFileType());
		assertEquals("the name should be the same in the cloned entry", entry.getName(), entry2.getName());
		assertEquals("the mode should be the same in the cloned entry", entry.getMode(), entry2.getMode());
		assertEquals("the size should be the same in the cloned entry", entry.getSize(), entry2.getSize());
		assertEquals("the time should be the same in the cloned entry", entry.getTime(), entry2.getTime());
	}
	
	/**
	 * Tests the {@link TarEntry#setFileType(int)} method
	 */
	public void testSetFileType() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals("type should be FILE by default", TarEntry.FILE, entry.getFileType());
		entry.setFileType(TarEntry.DIRECTORY);
		assertEquals("type should be DIRECTORY", TarEntry.DIRECTORY, entry.getFileType());
	}
	
	/**
	 * Tests the {@link TarEntry#setMode(long)} method
	 */
	public void testSetMode() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals("mode should be 0644 by default", 0644, entry.getMode());
		entry.setMode(1L);
		assertEquals("type should be 1L", 1L, entry.getMode());
	}
	
	/**
	 * Tests the {@link TarEntry#setSize(long)} method
	 */
	public void testSetSize() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals("size should be 0", 0, entry.getSize());
		entry.setSize(1L);
		assertEquals("size should be 1L", 1L, entry.getSize());
	}
	
	/**
	 * Tests the {@link TarEntry#setTime(long)} method
	 */
	public void testSetTime() {
		TarEntry entry = new TarEntry(TAR_NAME);
		entry.setTime(1L);
		assertEquals("Time should be 1L", 1L, entry.getTime());
	}
	
	/**
	 * Tests the {@link TarEntry#toString()} method
	 */
	public void testToString() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals("toString should return the name", TAR_NAME, entry.toString());
	}
}
