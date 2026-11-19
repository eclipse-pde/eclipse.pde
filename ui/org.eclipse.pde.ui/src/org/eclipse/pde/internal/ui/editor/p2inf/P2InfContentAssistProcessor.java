/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.p2inf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.core.builders.P2InfGrammar;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeCompletionProposal;
import org.eclipse.pde.internal.ui.editor.contentassist.TypePackageCompletionProcessor;

/**
 * Content-assist processor for {@code p2.inf} files. Suggestions are driven by
 * the key grammar of {@link P2InfGrammar}, which is shared with the validation
 * of p2.inf files.
 */
public class P2InfContentAssistProcessor extends TypePackageCompletionProcessor {

	protected PDESourcePage fSourcePage;

	public P2InfContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			String value = doc.get(lineStart, offset - lineStart).trim();
			List<TypeCompletionProposal> completions = new ArrayList<>();
			String sep = (value.isEmpty() || value.endsWith(".")) ? "" : "."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for (P2InfGrammar.Node node : getSuggestions(value)) {
				if (node.isIndex()) {
					String proposalText = value + sep + "0."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
					continue;
				}
				for (String name : node.getNames()) {
					if (node.isTerminal() && !node.getChildren().isEmpty()) {
						String bareProposal = value + sep + name;
						completions.add(new TypeCompletionProposal(bareProposal, null, bareProposal, lineStart, value.length()));
						String subkeyProposal = value + sep + name + "."; //$NON-NLS-1$
						completions.add(new TypeCompletionProposal(subkeyProposal, null, subkeyProposal, lineStart, value.length()));
					} else {
						String proposalText = value + sep + name + (node.isTerminal() ? "" : "."); //$NON-NLS-1$ //$NON-NLS-2$
						completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
					}
				}
			}
			return completions.toArray(ICompletionProposal[]::new);
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
		}
		return null;
	}

	private List<P2InfGrammar.Node> getSuggestions(String line) {
		P2InfGrammar.Node current = P2InfGrammar.getRoot();
		if (line.isEmpty()) {
			return current.getChildren();
		}
		for (String token : line.split("\\.")) { //$NON-NLS-1$
			if (token.isEmpty()) {
				continue;
			}
			P2InfGrammar.Node index = current.indexChild();
			P2InfGrammar.Node next = index != null ? (token.matches("\\d+") ? index : null) : current.child(token); //$NON-NLS-1$
			if (next == null) {
				return List.of();
			}
			current = next;
		}
		return current.getChildren();
	}
}
