/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.context;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.pde.internal.ui.editor.text.ManifestPartitionScanner;

public class ManifestDocumentSetupParticipant implements IDocumentSetupParticipant {

	public void setup(IDocument document) {
		IDocumentPartitioner partitioner = createDocumentPartitioner();
		if (partitioner != null) {
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
	}

	private IDocumentPartitioner createDocumentPartitioner() {
		return new FastPartitioner(new ManifestPartitionScanner(), ManifestPartitionScanner.PARTITIONS);
	}

}
