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
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeCompletionProposal;
import org.eclipse.pde.internal.ui.editor.contentassist.TypePackageCompletionProcessor;

public class P2InfContentAssistProcessor extends TypePackageCompletionProcessor {

	protected PDESourcePage fSourcePage;
	private SuggestionNode root;
	private static final String[] COMMON_PARTS = { Messages.P2InfHeader_namespace, Messages.P2InfHeader_name,
			Messages.P2InfHeader_version, Messages.P2InfHeader_range, Messages.P2InfHeader_matchExp,
			Messages.P2InfHeader_greedy, Messages.P2InfHeader_optional, Messages.P2InfHeader_multiple,
			Messages.P2InfHeader_filter };

	public P2InfContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
		buildSuggestionTree();
	}

	private static class SuggestionNode {
		String key;
		boolean index;
		boolean terminal;
		List<SuggestionNode> children = new ArrayList<>();
		SuggestionNode(String key) {
			this.key = key;
		}
		SuggestionNode index() {
			this.index = true;
			return this;
		}
		SuggestionNode terminal() {
			this.terminal = true;
			return this;
		}
		SuggestionNode addChild(SuggestionNode node) {
			children.add(node);
			return this;
		}
	}

	// Build Suggestion Tree
	private void buildSuggestionTree() {
		root = new SuggestionNode("root"); //$NON-NLS-1$

		SuggestionNode provides = new SuggestionNode(Messages.P2InfHeader_provides);
		SuggestionNode requires = new SuggestionNode(Messages.P2InfHeader_requires);
		SuggestionNode metaReq = new SuggestionNode(Messages.P2InfHeader_metaRequirements);
		SuggestionNode properties = new SuggestionNode(Messages.P2InfHeader_properties);
		SuggestionNode update = new SuggestionNode(Messages.P2InfHeader_update);
		SuggestionNode instructions = new SuggestionNode(Messages.P2InfHeader_instructions);
		SuggestionNode units = new SuggestionNode(Messages.P2InfHeader_units);

		root.addChild(provides).addChild(requires).addChild(metaReq).addChild(properties).addChild(update)
				.addChild(instructions).addChild(units);

		// Level-1 structures
		addIndexedParts(provides);
		addIndexedParts(requires);
		addIndexedParts(metaReq);
		addIndexedParts(properties);
		addIndexedParts(update);
		addIndexedParts(instructions);

		// units.{#}
		SuggestionNode unitsIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		units.addChild(unitsIndex);
		SuggestionNode unitRequires = new SuggestionNode(Messages.P2InfHeader_requires);
		SuggestionNode unitProvides = new SuggestionNode(Messages.P2InfHeader_provides);
		SuggestionNode unitProperties = new SuggestionNode(Messages.P2InfHeader_properties);
		SuggestionNode unitMetaRed = new SuggestionNode(Messages.P2InfHeader_metaRequirements);
		SuggestionNode unitUpdate = new SuggestionNode(Messages.P2InfHeader_update);
		SuggestionNode unitInstructions = new SuggestionNode(Messages.P2InfHeader_instructions);
		unitsIndex.addChild(unitRequires);
		unitsIndex.addChild(unitProvides);
		unitsIndex.addChild(unitProperties);
		unitsIndex.addChild(unitMetaRed);
		unitsIndex.addChild(unitUpdate);
		unitsIndex.addChild(unitInstructions);

		addIndexedParts(unitRequires);
		addIndexedParts(unitProvides);
		addIndexedParts(unitProperties);
		addIndexedParts(unitMetaRed);
		addIndexedParts(unitUpdate);
		addIndexedParts(unitInstructions);
	}

	private void addIndexedParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		for (String p : COMMON_PARTS) {
			indexNode.addChild(new SuggestionNode(p).terminal());
		}
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			String value = doc.get(lineStart, offset - lineStart).trim();
			List<TypeCompletionProposal> completions = new ArrayList<>();
			List<SuggestionNode> suggestions = getSuggestions(value);
			for (SuggestionNode node : suggestions) {
				String proposalText;
				if (node.index) {
					proposalText = value + "0."; //$NON-NLS-1$
				} else if (node.terminal) {
					proposalText = value + node.key;
				} else {
					proposalText = value + node.key + "."; //$NON-NLS-1$
				}
				completions
						.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
			}
			return completions.toArray(ICompletionProposal[]::new);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<SuggestionNode> getSuggestions(String line) {
		if (line.isEmpty()) {
			return root.children;
		}
		String[] tokens = line.split("\\."); //$NON-NLS-1$
		SuggestionNode current = root;
		for (String token : tokens) {
			if (token.isEmpty())
				continue;
			SuggestionNode next = null;
			for (SuggestionNode child : current.children) {
				if (child.index && token.matches("\\d+")) { //$NON-NLS-1$
					next = child;
					break;
				}
				if (!child.index && child.key.equals(token)) {
					next = child;
					break;
				}
			}
			if (next == null)
				return new ArrayList<>();
			current = next;
		}
		return current.children;
	}
}

