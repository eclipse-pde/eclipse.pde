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

public class Bug527084CompletionWithCommentsTest extends AbstractTargetEditorTest {
	private ITextViewer textViewer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		textViewer = getTextViewerForTarget("CommentsTestCaseTarget");
	}

	public void testTagNameCompletion() throws Exception {
		confirmCompletionAtOffset(89, "unit");
	}

	public void testTagValueCompletion() throws Exception {
		confirmCompletionAtOffset(219, "linux");
	}

	public void testAttributeNameCompletion() throws Exception {
		confirmCompletionAtOffset(85, "version");
	}

	public void testAttributeValueCompletion() throws Exception {
		confirmCompletionAtOffset(83, "Add repository URL first.");
	}

	private void confirmCompletionAtOffset(int offset, String expectedCompletion) throws Exception {
		checkProposals(new String[] { expectedCompletion },
				contentAssist.computeCompletionProposals(textViewer, offset + 1),
				offset);
	}
}
