/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.pde.internal.core.plugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


/**
 * SourceRange.java
 */
public class SourceRange implements ISourceRange {
	
	private int fStartLine= -1;
	private int fStartColumn= -1; 
	private int fEndLine= -1;
	private int fEndColumn= -1;
	
	/**
	 * Constructor SourceRange.
	 */
	public SourceRange() {
	}

	public SourceRange(int startLine, int startColumn, int endLine, int endColumn) {
		fStartLine= startLine;
		fStartColumn= startColumn;
		fEndLine= endLine;
		fEndColumn= endColumn;
	}

	public static int getOffset(IDocument document, int line, int column) throws BadLocationException {
		return document.getLineOffset(line-1) + column-1;
	}

	public int getStartOffset(IDocument document) throws BadLocationException {
		return getOffset(document, fStartLine, fStartColumn);
	}

	public int getEndOffset(IDocument document) throws BadLocationException {
		return getOffset(document, fEndLine, fEndColumn);
	}

	/**
	 * Returns the fEndColumn.
	 * @return int
	 */
	public int getEndColumn() {
		return fEndColumn;
	}

	/**
	 * Returns the fEndLine.
	 * @return int
	 */
	public int getEndLine() {
		return fEndLine;
	}

	/**
	 * Returns the fStartColumn.
	 * @return int
	 */
	public int getStartColumn() {
		return fStartColumn;
	}

	/**
	 * Returns the fStartLine.
	 * @return int
	 */
	public int getStartLine() {
		return fStartLine;
	}

	/**
	 * Sets the fEndColumn.
	 * @param fEndColumn The fEndColumn to set
	 */
	public void setEndColumn(int fEndColumn) {
		this.fEndColumn= fEndColumn;
	}

	/**
	 * Sets the fEndLine.
	 * @param fEndLine The fEndLine to set
	 */
	public void setEndLine(int fEndLine) {
		this.fEndLine= fEndLine;
	}

	/**
	 * Sets the fStartColumn.
	 * @param fStartColumn The fStartColumn to set
	 */
	public void setStartColumn(int fStartColumn) {
		this.fStartColumn= fStartColumn;
	}

	/**
	 * Sets the fStartLine.
	 * @param fStartLine The fStartLine to set
	 */
	public void setStartLine(int fStartLine) {
		this.fStartLine= fStartLine;
	}
}
