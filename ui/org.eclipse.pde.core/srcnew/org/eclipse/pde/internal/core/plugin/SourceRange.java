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

package org.eclipse.pde.internal.core.plugin;


/**
 * SourceRange.java
 */
public class SourceRange implements ISourceRange {
	
	private int fOffset = -1;
	private int fLength = -1;
	private int fStartLine = -1;
	private int fEndLine = -1;
	
	/**
	 * @return Returns the endLine.
	 */
	public int getEndLine() {
		return fEndLine;
	}

	/**
	 * @param endLine The endLine to set.
	 */
	public void setEndLine(int endLine) {
		this.fEndLine = endLine;
	}

	/**
	 * @return Returns the length.
	 */
	public int getLength() {
		return fLength;
	}

	/**
	 * @param length The length to set.
	 */
	public void setLength(int length) {
		this.fLength = length;
	}

	/**
	 * @return Returns the offset.
	 */
	public int getOffset() {
		return fOffset;
	}

	/**
	 * @param offset The offset to set.
	 */
	public void setOffset(int offset) {
		this.fOffset = offset;
	}

	/**
	 * @return Returns the startLine.
	 */
	public int getStartLine() {
		return fStartLine;
	}

	/**
	 * @param startLine The startLine to set.
	 */
	public void setStartLine(int startLine) {
		this.fStartLine = startLine;
	}

}
