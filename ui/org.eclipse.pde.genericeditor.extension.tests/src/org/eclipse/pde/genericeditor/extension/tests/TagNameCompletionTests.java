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
package org.eclipse.pde.genericeditor.extension.tests;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Assert;

public class TagNameCompletionTests extends AbstractTargetEditorTest {
	public void testTagNameSuggestions() throws Exception {
		Map<Integer, String[]> expectedProposalsByOffset = new HashMap<>();
		// locations
		expectedProposalsByOffset.put(1, new String[] {});
		expectedProposalsByOffset.put(12, new String[] { "targetJRE" });
		expectedProposalsByOffset.put(26, new String[] { "location" });
		// location, full
		expectedProposalsByOffset.put(39, new String[] { "unit" });
		expectedProposalsByOffset.put(63, new String[] { "unit" });
		// location, empty
		expectedProposalsByOffset.put(88, new String[] { "unit", "repository" });
		// environment
		expectedProposalsByOffset.put(129, new String[] { "os", "ws", "arch", "nl" });
		// launcherArgs
		expectedProposalsByOffset.put(161, new String[] { "vmArgs", "programArgs" });
		// target
		expectedProposalsByOffset.put(179, new String[] { "targetJRE" });

		ITextViewer textViewer = getTextViewerForTarget("TagNamesTestCaseTarget");

		for (int offset : expectedProposalsByOffset.keySet()) {
			ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer,
					offset);
			if (expectedProposalsByOffset.containsKey(offset)) {
				checkProposals(expectedProposalsByOffset.get(offset), completionProposals, offset);
			} else if (completionProposals.length != 0) {
				Assert.fail("There should not be any proposals at index " + offset + ". Following proposals found: "
						+ proposalListToString(completionProposals));
			}
		}
	}

	public void testNoTagNameRepeatSuggestions() throws Exception {
		ITextViewer textViewer = getTextViewerForTarget("TagNamesFullTestCaseTarget");
		IDocument document = textViewer.getDocument();
		String text = document.get();

		int offset = 0;
		while (offset < text.length()) {
			offset = text.indexOf("\n<\n", offset);
			if (offset == -1) {
				break;
			}
			ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer, offset);
			if (completionProposals.length != 0) {
				Assert.fail("There should not be any proposals at index " + offset + ". Following proposals found: "
						+ proposalListToString(completionProposals));
			}
			offset++;
		}
	}
}