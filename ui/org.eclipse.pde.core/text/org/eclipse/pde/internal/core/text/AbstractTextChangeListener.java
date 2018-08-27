/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text;

import java.util.HashMap;
import java.util.LinkedHashMap;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.text.edits.TextEdit;

public abstract class AbstractTextChangeListener implements IModelTextChangeListener {

	protected HashMap<Object, TextEdit> fOperationTable = new LinkedHashMap<>();
	protected IDocument fDocument;
	protected String fSep;

	public AbstractTextChangeListener(IDocument document) {
		fDocument = document;
		fSep = TextUtilities.getDefaultLineDelimiter(fDocument);
	}

}
