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

import org.eclipse.jface.text.ITextViewer;
import org.junit.Before;
import org.junit.Test;

public class Bug527084CompletionWithCommentsTest extends AbstractTargetEditorTest {
	private ITextViewer textViewer;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		textViewer = getTextViewerForTarget("CommentsTestCaseTarget");
	}

	@Test
	public void testTagNameCompletion() {
		confirmCompletionAtOffset(89, "unit");
	}

	@Test
	public void testTagValueCompletion() {
		confirmCompletionAtOffset(219, "linux");
	}

	@Test
	public void testAttributeNameCompletion() {
		confirmCompletionAtOffset(85, "version");
	}

	@Test
	public void testAttributeValueCompletion() {
		confirmCompletionAtOffset(83, "Add repository URL first.");
	}

	private void confirmCompletionAtOffset(int offset, String expectedCompletion) {
		checkProposals(new String[] { expectedCompletion },
				contentAssist.computeCompletionProposals(textViewer, offset + 1),
				offset);
	}
}
