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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Assert;

public class TagNameCompletionTests extends AbstractTargetEditorTest {
	public void testAttributeNameSuggestions() throws Exception {
		Map<Integer, String[]> expectedProposalsByOffset = new HashMap<>();
		// locations
		expectedProposalsByOffset.put(0, new String[] {});
		expectedProposalsByOffset.put(10, new String[] { "targetJRE" });
		expectedProposalsByOffset.put(23, new String[] { "location" });
		// location, full
		expectedProposalsByOffset.put(35, new String[] { "unit" });
		expectedProposalsByOffset.put(58, new String[] { "unit" });
		// location, empty
		expectedProposalsByOffset.put(82, new String[] { "unit", "repository" });
		// environment
		expectedProposalsByOffset.put(122, new String[] { "os", "ws", "arch", "nl" });
		// launcherArgs
		expectedProposalsByOffset.put(153, new String[] { "vmArgs", "programArgs" });
		// target
		expectedProposalsByOffset.put(170, new String[] { "targetJRE" });

		ITextViewer textViewer = getTextViewerForTarget("TagNamesTestCaseTarget");
		IDocument document = textViewer.getDocument();
		String text = document.get();

		for (int offset : expectedProposalsByOffset.keySet()) {
			document.replace(offset, 0, "<");
			textViewer.setDocument(document);

			ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer,
					offset + 1);
			if (expectedProposalsByOffset.containsKey(offset)) {
				checkProposals(expectedProposalsByOffset.get(offset), completionProposals, offset);
			} else if (completionProposals.length != 0) {
				Assert.fail("There should not be any proposals at index " + offset + ". Following proposals found: "
						+ proposalListToString(completionProposals));
			}
			document.set(text);
		}
	}

	public void testNoAttributeNameRepeatSuggestions() throws Exception {
		ITextViewer textViewer = getTextViewerForTarget("TagNamesFullTestCaseTarget");
		IDocument document = textViewer.getDocument();
		String text = document.get();

		int offset = 0;
		while (offset < text.length()) {
			offset = text.indexOf("\n\n", offset);
			if (offset == -1) {
				break;
			}
			document.replace(offset, 0, "<");
			textViewer.setDocument(document);
			offset++;

			ICompletionProposal[] completionProposals = contentAssist.computeCompletionProposals(textViewer, offset);
			if (completionProposals.length != 0) {
				Assert.fail("There should not be any proposals at index " + offset + ". Following proposals found: "
						+ proposalListToString(completionProposals));
			}
			document.set(text);
		}
	}
}