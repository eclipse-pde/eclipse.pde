/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
