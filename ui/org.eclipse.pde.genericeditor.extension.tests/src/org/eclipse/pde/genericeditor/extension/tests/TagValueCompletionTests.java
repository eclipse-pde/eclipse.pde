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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TagValueCompletionTests extends AbstractTargetEditorTest {

	private List<Integer> expectedCompletionOffsets;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		expectedCompletionOffsets = new ArrayList<>();
		// os, ws, arch, nl
		expectedCompletionOffsets.add(27);
		expectedCompletionOffsets.add(37);
		expectedCompletionOffsets.add(49);
		expectedCompletionOffsets.add(61);
	}

	@Test
	public void testAttributeNameSuggestions() throws Exception {
		ITextViewer textViewer = getTextViewerForTarget("TagValuesTestCaseTarget");
		String text = textViewer.getDocument().get();
		int offset = 0;
		while (offset < text.length()) {
			int nextClose = text.indexOf('>', offset);
			int nextNewLine = text.indexOf('\n', offset);
			if (nextClose == 0 && nextNewLine == -1)
				break;
			if (nextClose == 0) {
				offset = nextNewLine;
			} else if (nextNewLine == -1) {
				offset = nextClose;
			} else {
				offset = Math.min(nextClose, nextNewLine);
			}

			ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer, offset);
			if (completionProposals.length > 0 && !expectedCompletionOffsets.contains(offset)) {
				Assert.fail("There should not be any proposals at index " + offset + ". Following proposals found: "
						+ proposalListToString(completionProposals));
			}
			offset++;
		}
	}

}
