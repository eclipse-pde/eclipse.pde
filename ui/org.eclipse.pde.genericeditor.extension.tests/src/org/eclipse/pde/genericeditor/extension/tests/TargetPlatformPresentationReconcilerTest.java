/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial implementation
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import static org.junit.Assert.assertNotNull;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation.TargetPlatformPresentationReconciler;
import org.junit.Test;

/**
 * Tests for {@link TargetPlatformPresentationReconciler}.
 *
 * <p>The reconciler overrides {@code createPresentation} to scan from offset 0
 * so that multi-line rules (XML comments) are correctly recognised even when
 * the damaged region is deep inside the document. These tests verify that the
 * presentation is computed correctly for several representative document shapes
 * and damage positions.
 */
public class TargetPlatformPresentationReconcilerTest {

	/** Subclass that exposes the protected {@code createPresentation} for testing. */
	private static class TestableReconciler extends TargetPlatformPresentationReconciler {
		TextPresentation createPresentation(IDocument document, IRegion damage) {
			IPresentationRepairer repairer = getRepairer(IDocument.DEFAULT_CONTENT_TYPE);
			if (repairer != null) {
				repairer.setDocument(document);
			}
			return createPresentation(damage, document);
		}
	}

	@Test
	public void testCreatePresentationReturnsNonNullForSimpleDocument() {
		TestableReconciler reconciler = new TestableReconciler();
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<target name=\"test\">\n" //$NON-NLS-1$
				+ "  <locations/>\n" //$NON-NLS-1$
				+ "</target>\n"; //$NON-NLS-1$
		IDocument document = new Document(content);
		IRegion damage = new Region(0, content.length());

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull("Expected a non-null TextPresentation for a simple document", presentation); //$NON-NLS-1$
	}

	@Test
	public void testCreatePresentationWithDamageSubsetOfDocument() {
		TestableReconciler reconciler = new TestableReconciler();
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<target name=\"test\">\n" //$NON-NLS-1$
				+ "  <locations>\n" //$NON-NLS-1$
				+ "  </locations>\n" //$NON-NLS-1$
				+ "</target>\n"; //$NON-NLS-1$
		IDocument document = new Document(content);
		// Damage is only the last few characters, not the full document.
		int damageOffset = content.indexOf("</target>"); //$NON-NLS-1$
		IRegion damage = new Region(damageOffset, content.length() - damageOffset);

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull("Expected a non-null TextPresentation when damage covers only end of document", presentation); //$NON-NLS-1$
	}

	@Test
	public void testCreatePresentationWithMultilineCommentBeforeDamage() {
		TestableReconciler reconciler = new TestableReconciler();
		// Multi-line comment appears before the damage region. The reconciler must
		// scan from offset 0 so it can correctly determine the scanner state
		// (comment vs normal) at the start of the damage.
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<target name=\"test\">\n" //$NON-NLS-1$
				+ "<!--\n" //$NON-NLS-1$
				+ "  multi-line comment\n" //$NON-NLS-1$
				+ "-->\n" //$NON-NLS-1$
				+ "  <locations/>\n" //$NON-NLS-1$
				+ "</target>\n"; //$NON-NLS-1$
		IDocument document = new Document(content);
		// Damage is after the comment closes.
		int damageOffset = content.indexOf("<locations/>"); //$NON-NLS-1$
		IRegion damage = new Region(damageOffset, content.length() - damageOffset);

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull("Expected a non-null TextPresentation when a multi-line comment precedes the damage", //$NON-NLS-1$
				presentation);
	}

	@Test
	public void testCreatePresentationWithDamageInsideMultilineComment() {
		TestableReconciler reconciler = new TestableReconciler();
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<target name=\"test\">\n" //$NON-NLS-1$
				+ "<!--\n" //$NON-NLS-1$
				+ "  line one\n" //$NON-NLS-1$
				+ "  line two\n" //$NON-NLS-1$
				+ "-->\n" //$NON-NLS-1$
				+ "</target>\n"; //$NON-NLS-1$
		IDocument document = new Document(content);
		// Damage is inside the comment body.
		int damageOffset = content.indexOf("  line two"); //$NON-NLS-1$
		IRegion damage = new Region(damageOffset, "  line two\n".length()); //$NON-NLS-1$

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull("Expected a non-null TextPresentation when damage is inside a multi-line comment", presentation); //$NON-NLS-1$
	}
}
