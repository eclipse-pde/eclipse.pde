/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.context;

import org.eclipse.core.filebuffers.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public class XMLDocumentSetupParticpant implements IDocumentSetupParticipant {

	public void setup(IDocument document) {
		IDocumentPartitioner partitioner = createDocumentPartitioner();
		if (partitioner != null) {
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
	}
	
	private IDocumentPartitioner createDocumentPartitioner() {
		FastPartitioner partitioner = new FastPartitioner(
				new XMLPartitionScanner(), new String[]{
						XMLPartitionScanner.XML_TAG,
						XMLPartitionScanner.XML_COMMENT});
		return partitioner;
	}

}
