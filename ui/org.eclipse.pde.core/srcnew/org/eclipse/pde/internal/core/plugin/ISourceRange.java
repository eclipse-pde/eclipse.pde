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
 * ISourceRange.java
 */
public interface ISourceRange {
	
	int getOffset();
	int getLength();
	int getStartLine();
	int getEndLine();
	
	void setOffset(int offset);
	void setLength(int length);
	void setStartLine(int line);
	void setEndLine(int line);
}
