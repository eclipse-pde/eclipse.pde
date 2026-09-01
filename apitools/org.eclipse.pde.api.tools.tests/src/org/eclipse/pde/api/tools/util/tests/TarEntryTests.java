/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.pde.api.tools.internal.util.TarEntry;
import org.junit.jupiter.api.Test;

/**
 * Test The {@link org.eclipse.pde.api.tools.internal.util.TarEntry} class
 *
 * @since 1.0.1
 */
public class TarEntryTests {

	static final String TAR_NAME = "tar_file"; //$NON-NLS-1$

	/**
	 * Tests the constructor
	 */
	@Test
	public void testConstructors() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals(0644, entry.getMode(), "mode should be 0644"); //$NON-NLS-1$
		assertEquals(TAR_NAME, entry.getName(), "name sould be 'foo'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link TarEntry#clone()} method
	 */
	@Test
	public void testClone() {
		TarEntry entry = new TarEntry(TAR_NAME);
		TarEntry entry2 = (TarEntry) entry.clone();
		assertNotNull(entry2, "The object should have been cloned"); //$NON-NLS-1$
		assertEquals(entry.getFileType(), entry2.getFileType(), "the file type should be the same in the cloned entry"); //$NON-NLS-1$
		assertEquals(entry.getName(), entry2.getName(), "the name should be the same in the cloned entry"); //$NON-NLS-1$
		assertEquals(entry.getMode(), entry2.getMode(), "the mode should be the same in the cloned entry"); //$NON-NLS-1$
		assertEquals(entry.getSize(), entry2.getSize(), "the size should be the same in the cloned entry"); //$NON-NLS-1$
		assertEquals(entry.getTime(), entry2.getTime(), "the time should be the same in the cloned entry"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link TarEntry#setFileType(int)} method
	 */
	@Test
	public void testSetFileType() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals(TarEntry.FILE, entry.getFileType(), "type should be FILE by default"); //$NON-NLS-1$
		entry.setFileType(TarEntry.DIRECTORY);
		assertEquals(TarEntry.DIRECTORY, entry.getFileType(), "type should be DIRECTORY"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link TarEntry#setMode(long)} method
	 */
	@Test
	public void testSetMode() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals(0644, entry.getMode(), "mode should be 0644 by default"); //$NON-NLS-1$
		entry.setMode(1L);
		assertEquals(1L, entry.getMode(), "type should be 1L"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link TarEntry#setSize(long)} method
	 */
	@Test
	public void testSetSize() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals(0, entry.getSize(), "size should be 0"); //$NON-NLS-1$
		entry.setSize(1L);
		assertEquals(1L, entry.getSize(), "size should be 1L"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link TarEntry#setTime(long)} method
	 */
	@Test
	public void testSetTime() {
		TarEntry entry = new TarEntry(TAR_NAME);
		entry.setTime(1L);
		assertEquals(1L, entry.getTime(), "Time should be 1L"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link TarEntry#toString()} method
	 */
	@Test
	public void testToString() {
		TarEntry entry = new TarEntry(TAR_NAME);
		assertEquals(TAR_NAME, entry.toString(), "toString should return the name"); //$NON-NLS-1$
	}
}
