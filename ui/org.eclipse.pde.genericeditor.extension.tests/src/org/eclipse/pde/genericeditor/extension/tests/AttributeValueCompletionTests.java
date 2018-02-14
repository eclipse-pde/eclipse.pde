/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import static org.junit.Assert.assertTrue;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Test;

public class AttributeValueCompletionTests extends AbstractTargetEditorTest {
	@Test
	public void testAttributeNameSuggestions() throws Exception {
		final int unitIdOffset = 42;
		final int unitVersionOffset = 53;
		ITextViewer textViewer = getTextViewerForTarget("AttributeValuesTestCaseTarget");
		String expectedValueString = "Add repository URL first.";
		ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer, unitIdOffset);
		assertTrue(completionProposals.length == 1
				&& completionProposals[0].getDisplayString().equals(expectedValueString));

		completionProposals = contentAssist.computeCompletionProposals(textViewer, unitVersionOffset);
		assertTrue(completionProposals.length == 1
				&& completionProposals[0].getDisplayString().equals(expectedValueString));
	}
}
