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

package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.pde.internal.ui.editor.SystemFileDocumentProvider;


public class SynchronizedSystemFileDocumentProvider extends SystemFileDocumentProvider {
	public SynchronizedSystemFileDocumentProvider(IDocumentPartitioner partitioner) {
		super(partitioner);
	}
	public SynchronizedSystemFileDocumentProvider(IDocumentPartitioner partitioner, String encoding) {
		super(partitioner, encoding);
	}
	public IDocument createEmptyDocument(Object element) {
		return new PartiallySynchronizedDocument();
	}
}
