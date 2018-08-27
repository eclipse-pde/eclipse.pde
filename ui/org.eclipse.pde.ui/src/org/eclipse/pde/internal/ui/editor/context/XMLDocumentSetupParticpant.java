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
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
import org.eclipse.pde.internal.ui.editor.text.XMLStringPartitionScanner;

/**
 * Creates and sets up the document partitioner
 *
 */
public class XMLDocumentSetupParticpant implements IDocumentSetupParticipant {

	public static final String XML_PARTITIONING = "_pde_xml_partitioning"; //$NON-NLS-1$

	@Override
	public void setup(IDocument document) {
		IDocumentPartitioner partitioner = createDocumentPartitioner();
		if (partitioner != null) {
			partitioner.connect(document);
			if (document instanceof IDocumentExtension3) {
				IDocumentExtension3 de3 = (IDocumentExtension3) document;
				de3.setDocumentPartitioner(XML_PARTITIONING, partitioner);
				partitioner = new FastPartitioner(new XMLStringPartitionScanner(), XMLStringPartitionScanner.STRING_PARTITIONS);
				partitioner.connect(document);
				de3.setDocumentPartitioner(XMLStringPartitionScanner.XML_STRING, partitioner);
			} else {
				document.setDocumentPartitioner(partitioner);
			}
		}
	}

	private IDocumentPartitioner createDocumentPartitioner() {
		return new FastPartitioner(new XMLPartitionScanner(), XMLPartitionScanner.PARTITIONS);
	}

}
