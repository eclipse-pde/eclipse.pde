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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Assert;
import org.junit.Test;

public class TagNameCompletionTests extends AbstractTargetEditorTest {
	@Test
	public void testTagNameSuggestions() throws Exception {
		Map<Integer, String[]> expectedProposalsByOffset = new HashMap<>();
		// locations
		expectedProposalsByOffset.put(1, new String[] {});
		expectedProposalsByOffset.put(12, new String[] { "targetJRE" });
		expectedProposalsByOffset.put(26, new String[] { "location (Directory)", "location (Feature)",
				"location (Installable Unit)", "location (Profile)" });
		// location, full
		expectedProposalsByOffset.put(39, new String[] { "unit" });
		expectedProposalsByOffset.put(63, new String[] { "unit" });
		// location, empty
		expectedProposalsByOffset.put(88, new String[] { "repository", "unit" });
		// environment
		expectedProposalsByOffset.put(129, new String[] { "arch", "nl", "os", "ws" });
		// launcherArgs
		expectedProposalsByOffset.put(161, new String[] { "programArgs", "vmArgs" });
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

	@Test
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