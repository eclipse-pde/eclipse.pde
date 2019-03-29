/*******************************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc. and others
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

import org.eclipse.jface.text.ITextViewer;
import org.junit.Before;
import org.junit.Test;

public class Bug528706CompletionWithMultilineTagsTest extends AbstractTargetEditorTest {
	private ITextViewer textViewer;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		textViewer = getTextViewerForTarget("MultilineTagTestCaseTarget");
	}

	@Test
	public void testTagNameCompletionBeforeAMultiline() {
		confirmCompletionAtOffset(24, "location (Profile)");
	}

	@Test
	public void testTagNameCompletionAfterAMultiline() {
		confirmCompletionAtOffset(115, "location (Profile)");
	}

	@Test
	public void testTagValueCompletionBeforeAMultiline() {
		confirmCompletionAtOffset(150, "cocoa");
	}

	@Test
	public void testTagValueCompletionAsAMultiline() {
		confirmCompletionAtOffset(166, "linux");
	}

	@Test
	public void testTagValueCompletionAfterAMultiline() {
		checkProposals(new String[] { "x86", "x86_64" },
				contentAssist.computeCompletionProposals(textViewer, 181 + 1), 181);
	}

	@Test
	public void testAttributeNameCompletionBeforeAMultiline() {
		confirmCompletionAtOffset(42, "id");
	}

	@Test
	public void testAttributeNameCompletionAsAMultiline() {
		confirmCompletionAtOffset(63, "id");
	}

	@Test
	public void testAttributeNameCompletionAfterAMultiline() {
		confirmCompletionAtOffset(85, "id");
	}

	@Test
	public void testAttributeValueCompletionBeforeAMultiline() {
		confirmCompletionAtOffset(52, "Add repository URL first.");
	}

	@Test
	public void testAttributeValueCompletionAsAMultiline() {
		confirmCompletionAtOffset(74, "Add repository URL first.");
	}

	@Test
	public void testAttributeValueCompletionAfterAMultiline() {
		confirmCompletionAtOffset(95, "Add repository URL first.");
	}

	private void confirmCompletionAtOffset(int offset, String expectedCompletion) {
		checkProposals(new String[] { expectedCompletion },
				contentAssist.computeCompletionProposals(textViewer, offset + 1), offset);
	}
}
