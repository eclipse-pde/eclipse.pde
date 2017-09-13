/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target.editor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class AttributeValueCompletionTests extends AbstractTargetEditorTest {
	public void testAttributeNameSuggestions() throws Exception {
		final int unitIdOffset = 42;
		final int unitVersionOffset = 53;
		ITextViewer textViewer = getTextViewerForTarget("AttributeValues");
		String expectedValueString = "Started job fetching metadata. Retry when job is finished";
		ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer, unitIdOffset);
		assertTrue(completionProposals.length == 1
				&& completionProposals[0].getDisplayString().equals(expectedValueString));

		completionProposals = contentAssist.computeCompletionProposals(textViewer, unitVersionOffset);
		assertTrue(completionProposals.length == 1
				&& completionProposals[0].getDisplayString().equals(expectedValueString));
	}
}
