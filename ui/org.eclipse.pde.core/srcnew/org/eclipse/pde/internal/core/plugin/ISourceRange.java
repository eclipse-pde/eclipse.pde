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
 * ISourceRange.java
 */
public interface ISourceRange {
	
	int getStartOffset(IDocument document) throws BadLocationException;
	int getEndOffset(IDocument document) throws BadLocationException;

	int getEndColumn();
	int getEndLine();
	int getStartColumn();
	int getStartLine();
	
	void setEndColumn(int fEndColumn);
	void setEndLine(int fEndLine);
	void setStartColumn(int fStartColumn);
	void setStartLine(int fStartLine);
}