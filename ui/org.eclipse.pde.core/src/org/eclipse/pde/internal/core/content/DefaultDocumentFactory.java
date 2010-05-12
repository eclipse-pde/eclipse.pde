/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.content;

import org.eclipse.core.filebuffers.*;
import org.eclipse.jface.text.IDocument;

public class DefaultDocumentFactory implements IDocumentFactory {

	/**
	 * Just ensures, that the default document is created.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=307524
	 */
	public IDocument createDocument() {
		return FileBuffers.getTextFileBufferManager().createEmptyDocument(null, LocationKind.LOCATION);
	}
}
