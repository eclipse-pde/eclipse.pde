package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.editor.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public abstract class PDEMultiPageXMLEditor extends PDEMultiPageEditor {

public PDEMultiPageXMLEditor() {
	super();
}
protected IDocumentPartitioner createDocumentPartitioner() {
	RuleBasedPartitioner partitioner =
		new RuleBasedPartitioner(
			new PDEPartitionScanner(),
			new String[] { PDEPartitionScanner.XML_TAG, PDEPartitionScanner.XML_COMMENT });
	return partitioner;
}
}
