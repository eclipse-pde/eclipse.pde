/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.util.HashMap;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.TextEdit;

public abstract class AbstractTextChangeListener implements IModelTextChangeListener {

	protected HashMap<Object, TextEdit> fOperationTable = new HashMap<Object, TextEdit>();
	protected IDocument fDocument;
	protected String fSep;

	public AbstractTextChangeListener(IDocument document) {
		fDocument = document;
		fSep = TextUtilities.getDefaultLineDelimiter(fDocument);
	}

}
