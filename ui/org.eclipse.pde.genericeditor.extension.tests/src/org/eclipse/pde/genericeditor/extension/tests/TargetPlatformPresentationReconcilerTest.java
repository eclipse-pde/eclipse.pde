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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation.TargetPlatformPresentationReconciler;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TargetPlatformPresentationReconciler}.
 *
 * <p>The reconciler overrides {@code createPresentation} to scan from offset 0
 * so that multi-line rules (XML comments) are correctly recognised even when
 * the damaged region is deep inside the document. The scan must stop at the
 * end of the damage region (not the end of the document), because style ranges
 * beyond the damage end are discarded anyway.
 *
 * <p>To make these tests meaningful regression tests for the performance fix,
 * the document records the {@code (offset, length)} passed to
 * {@code IDocumentExtension3.computePartitioning(String, int, int, boolean)} so we can
 * verify the scan actually stops at {@code damage.getOffset() + damage.getLength()}
 * rather than running all the way to {@code document.getLength()}.
 */
public class TargetPlatformPresentationReconcilerTest {

	/** Records the length passed to {@code computePartitioning} so tests can assert on it. */
	private static class RecordingDocument extends Document {
		int lastPartitioningOffset = -1;
		int lastPartitioningLength = -1;

		RecordingDocument(String content) {
			super(content);
		}

		@Override
		public ITypedRegion[] computePartitioning(String partitioning, int offset, int length,
				boolean includeZeroLengthPartitions) throws BadLocationException, BadPartitioningException {
			lastPartitioningOffset = offset;
			lastPartitioningLength = length;
			return super.computePartitioning(partitioning, offset, length, includeZeroLengthPartitions);
		}
	}

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

	private static void assertScanStopsAtDamageEnd(RecordingDocument document, IRegion damage) {
		assertEquals(0, document.lastPartitioningOffset,
				"Scan should start at offset 0 to preserve multi-line rule context"); //$NON-NLS-1$
		int expectedScanLength = Math.min(damage.getOffset() + damage.getLength(), document.getLength());
		assertEquals(expectedScanLength, document.lastPartitioningLength,
				"Scan should stop at damage end, not document end"); //$NON-NLS-1$
	}

	@Test
	public void testCreatePresentationReturnsNonNullForSimpleDocument() {
		TestableReconciler reconciler = new TestableReconciler();
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<target name=\"test\">\n" //$NON-NLS-1$
				+ "  <locations/>\n" //$NON-NLS-1$
				+ "</target>\n"; //$NON-NLS-1$
		RecordingDocument document = new RecordingDocument(content);
		IRegion damage = new Region(0, content.length());

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull(presentation, "Expected a non-null TextPresentation for a simple document"); //$NON-NLS-1$
		assertScanStopsAtDamageEnd(document, damage);
	}

	@Test
	public void testCreatePresentationWithDamageSubsetOfDocument() {
		TestableReconciler reconciler = new TestableReconciler();
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //$NON-NLS-1$
				+ "<target name=\"test\">\n" //$NON-NLS-1$
				+ "  <locations>\n" //$NON-NLS-1$
				+ "  </locations>\n" //$NON-NLS-1$
				+ "</target>\n"; //$NON-NLS-1$
		RecordingDocument document = new RecordingDocument(content);
		// Damage is only the last few characters, not the full document.
		int damageOffset = content.indexOf("</target>"); //$NON-NLS-1$
		IRegion damage = new Region(damageOffset, content.length() - damageOffset);

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull(presentation, "Expected a non-null TextPresentation when damage covers only end of document"); //$NON-NLS-1$
		assertScanStopsAtDamageEnd(document, damage);
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
		RecordingDocument document = new RecordingDocument(content);
		// Damage is after the comment closes.
		int damageOffset = content.indexOf("<locations/>"); //$NON-NLS-1$
		IRegion damage = new Region(damageOffset, content.length() - damageOffset);

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull(presentation,
				"Expected a non-null TextPresentation when a multi-line comment precedes the damage"); //$NON-NLS-1$
		assertScanStopsAtDamageEnd(document, damage);
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
		RecordingDocument document = new RecordingDocument(content);
		// Damage is inside the comment body.
		int damageOffset = content.indexOf("  line two"); //$NON-NLS-1$
		IRegion damage = new Region(damageOffset, "  line two\n".length()); //$NON-NLS-1$

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull(presentation, "Expected a non-null TextPresentation when damage is inside a multi-line comment"); //$NON-NLS-1$
		assertScanStopsAtDamageEnd(document, damage);
	}

	@Test
	public void testScanEndClampedToDocumentLength() {
		// Damage region extending past document end (can happen with a stale damage
		// during rapid edits) must be clamped to document length to avoid a
		// BadLocationException and preserve syntax highlighting.
		TestableReconciler reconciler = new TestableReconciler();
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<target/>\n"; //$NON-NLS-1$
		RecordingDocument document = new RecordingDocument(content);
		IRegion damage = new Region(0, content.length() + 100);

		TextPresentation presentation = reconciler.createPresentation(document, damage);

		assertNotNull(presentation, "Expected a non-null TextPresentation when damage extends past document end"); //$NON-NLS-1$
		assertEquals(content.length(), document.lastPartitioningLength,
				"Scan should be clamped to document length"); //$NON-NLS-1$
	}
}
