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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Assert;

public class TagValueCompletionTests extends AbstractTargetEditorTest {

	private List<Integer> expectedCompletionOffsets;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		expectedCompletionOffsets = new ArrayList<Integer>();
		// os, ws, arch, nl
		expectedCompletionOffsets.add(27);
		expectedCompletionOffsets.add(37);
		expectedCompletionOffsets.add(49);
		expectedCompletionOffsets.add(61);
	}

	public void testAttributeNameSuggestions() throws Exception {
		ITextViewer textViewer = getTextViewerForTarget("TagValues");
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
