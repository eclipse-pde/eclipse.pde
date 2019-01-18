/*******************************************************************************
 * Copyright (c) 2010, 2013 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.content;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IDocumentFactory;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.jface.text.IDocument;

public class DefaultDocumentFactory implements IDocumentFactory {

	/**
	 * Just ensures, that the default document is created.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=307524
	 */
	@Override
	@Deprecated
	public IDocument createDocument() {
		return FileBuffers.getTextFileBufferManager().createEmptyDocument(null, LocationKind.LOCATION);
	}
}
