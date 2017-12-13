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

public class Bug528706CompletionWithMultilineTagsTest extends AbstractTargetEditorTest {
	private ITextViewer textViewer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		textViewer = getTextViewerForTarget("MultilineTagTestCaseTarget");
	}

	public void testTagNameCompletionBeforeAMultiline() throws Exception {
		confirmCompletionAtOffset(21, "location");
	}

	public void testTagNameCompletionAfterAMultiline() throws Exception {
		confirmCompletionAtOffset(109, "location");
	}

	public void testTagValueCompletionBeforeAMultiline() throws Exception {
		confirmCompletionAtOffset(145, "carbon");
	}

	public void testTagValueCompletionAsAMultiline() throws Exception {
		confirmCompletionAtOffset(161, "linux");
	}

	public void testTagValueCompletionAfterAMultiline() throws Exception {
		confirmCompletionAtOffset(181, "PA_RISC");
	}

	public void testAttributeNameCompletionBeforeAMultiline() throws Exception {
		confirmCompletionAtOffset(39, "id");
	}

	public void testAttributeNameCompletionAsAMultiline() throws Exception {
		confirmCompletionAtOffset(60, "id");
	}

	public void testAttributeNameCompletionAfterAMultiline() throws Exception {
		confirmCompletionAtOffset(82, "id");
	}

	public void testAttributeValueCompletionBeforeAMultiline() throws Exception {
		confirmCompletionAtOffset(49, "Add repository URL first.");
	}

	public void testAttributeValueCompletionAsAMultiline() throws Exception {
		confirmCompletionAtOffset(71, "Add repository URL first.");
	}

	public void testAttributeValueCompletionAfterAMultiline() throws Exception {
		confirmCompletionAtOffset(92, "Add repository URL first.");
	}

	private void confirmCompletionAtOffset(int offset, String expectedCompletion) throws Exception {
		checkProposals(new String[] { expectedCompletion },
				contentAssist.computeCompletionProposals(textViewer, offset + 1),
				offset);
	}
}
