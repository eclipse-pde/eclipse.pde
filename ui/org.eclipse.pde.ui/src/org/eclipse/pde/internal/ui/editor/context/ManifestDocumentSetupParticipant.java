/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.context;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.pde.internal.ui.editor.text.ManifestPartitionScanner;

public class ManifestDocumentSetupParticipant implements IDocumentSetupParticipant {

	@Override
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
