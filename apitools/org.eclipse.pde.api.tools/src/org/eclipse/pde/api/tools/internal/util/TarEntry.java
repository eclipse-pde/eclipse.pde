/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.util;

/**
 * Representation of a file in a tar archive.
 */
public class TarEntry implements Cloneable {
	private final String name;
	private long mode, time, size;
	private int type;
	int filepos;

	/**
	 * Entry type for normal files.
	 */
	public static final int FILE = '0';

	/**
	 * Entry type for directories.
	 */
	public static final int DIRECTORY = '5';

	/**
	 * Create a new TarEntry for a file of the given name at the given position
	 * in the file.
	 *
	 * @param name filename
	 * @param pos position in the file in bytes
	 */
	TarEntry(String name, int pos) {
		this.name = name;
		mode = 0644;
		type = FILE;
		filepos = pos;
		time = System.currentTimeMillis() / 1000;
	}

	/**
	 * Create a new TarEntry for a file of the given name.
	 *
	 * @param name filename
	 */
	public TarEntry(String name) {
		this(name, -1);
	}

	@Override
	public Object clone() {
		TarEntry entry = new TarEntry(this.name, this.filepos);
		entry.setFileType(this.type);
		entry.setMode(this.mode);
		entry.setSize(this.size);
		entry.setTime(this.time);
		return entry;
	}

	/**
	 * Returns the type of this file, one of FILE, LINK, SYM_LINK, CHAR_DEVICE,
	 * BLOCK_DEVICE, DIRECTORY or FIFO.
	 *
	 * @return file type
	 */
	public int getFileType() {
		return type;
	}

	/**
	 * Returns the mode of the file in UNIX permissions format.
	 *
	 * @return file mode
	 */
	public long getMode() {
		return mode;
	}

	/**
	 * Returns the name of the file.
	 *
	 * @return filename
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the size of the file in bytes.
	 *
	 * @return size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Returns the modification time of the file in seconds since January 1st
	 * 1970.
	 *
	 * @return time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the type of the file, one of FILE, LINK, SYMLINK, CHAR_DEVICE,
	 * BLOCK_DEVICE, or DIRECTORY.
	 *
	 * @param type
	 */
	public void setFileType(int type) {
		this.type = type;
	}

	/**
	 * Sets the mode of the file in UNIX permissions format.
	 *
	 * @param mode
	 */
	public void setMode(long mode) {
		this.mode = mode;
	}

	/**
	 * Sets the size of the file in bytes.
	 *
	 * @param size
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Sets the modification time of the file in seconds since January 1st 1970.
	 *
	 * @param time
	 */
	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
