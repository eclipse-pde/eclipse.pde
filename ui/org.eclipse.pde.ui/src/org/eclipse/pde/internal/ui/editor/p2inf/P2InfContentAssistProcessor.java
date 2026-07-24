/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeCompletionProposal;
import org.eclipse.pde.internal.ui.editor.contentassist.TypePackageCompletionProcessor;

public class P2InfContentAssistProcessor extends TypePackageCompletionProcessor {

	protected PDESourcePage fSourcePage;
	private SuggestionNode root;
	private static final String[] REQUIRES_PARTS = { Messages.P2InfHeader_namespace, Messages.P2InfHeader_name,
			Messages.P2InfHeader_range, Messages.P2InfHeader_matchExp, Messages.P2InfHeader_greedy,
			Messages.P2InfHeader_optional, Messages.P2InfHeader_multiple, Messages.P2InfHeader_filter,
			Messages.P2InfHeader_min, Messages.P2InfHeader_max };

	private static final String[] META_REQUIRES_PARTS = { Messages.P2InfHeader_namespace, Messages.P2InfHeader_name,
			Messages.P2InfHeader_range, Messages.P2InfHeader_matchExp, Messages.P2InfHeader_greedy,
			Messages.P2InfHeader_optional, Messages.P2InfHeader_multiple };

	private static final String[] HOST_REQUIRES_PARTS = { Messages.P2InfHeader_namespace, Messages.P2InfHeader_name,
			Messages.P2InfHeader_range, Messages.P2InfHeader_greedy, Messages.P2InfHeader_optional,
			Messages.P2InfHeader_multiple };

	public P2InfContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
		buildSuggestionTree();
	}

	private static class SuggestionNode {
		private final String key;
		private boolean index;
		private boolean terminal;
		private final List<SuggestionNode> children = new ArrayList<>();

		SuggestionNode(String key) {
			this.key = key;
		}

		public SuggestionNode index() {
			this.index = true;
			return this;
		}

		public SuggestionNode terminal() {
			this.terminal = true;
			return this;
		}

		public SuggestionNode addChild(SuggestionNode node) {
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
		SuggestionNode iu = new SuggestionNode(Messages.P2InfHeader_iu);
		SuggestionNode units = new SuggestionNode(Messages.P2InfHeader_units);

		root.addChild(provides).addChild(requires).addChild(metaReq).addChild(properties).addChild(update)
				.addChild(instructions).addChild(iu).addChild(units);

		addProvidesParts(provides);
		addRequiresParts(requires);
		addMetaRequiresParts(metaReq);
		addPropertiesParts(properties);
		addUpdateParts(update);
		addInstructionsParts(instructions);

		SuggestionNode iuIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		iu.addChild(iuIndex);
		addUnitIndexParts(iuIndex);

		SuggestionNode unitsIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		units.addChild(unitsIndex);
		addUnitIndexParts(unitsIndex);
	}

	private void addUnitIndexParts(SuggestionNode indexNode) {
		indexNode.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		indexNode.addChild(new SuggestionNode("version").terminal()); //$NON-NLS-1$
		indexNode.addChild(new SuggestionNode("singleton").terminal()); //$NON-NLS-1$
		indexNode.addChild(new SuggestionNode("filter").terminal()); //$NON-NLS-1$

		SuggestionNode copyright = new SuggestionNode(Messages.P2InfHeader_copyright);
		copyright.addChild(new SuggestionNode(Messages.P2InfHeader_location).terminal());
		indexNode.addChild(copyright);

		SuggestionNode touchpoint = new SuggestionNode(Messages.P2InfHeader_touchpoint);
		touchpoint.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		touchpoint.addChild(new SuggestionNode("version").terminal()); //$NON-NLS-1$
		indexNode.addChild(touchpoint);

		SuggestionNode unitUpdate = new SuggestionNode(Messages.P2InfHeader_update);
		unitUpdate.addChild(new SuggestionNode(Messages.P2InfHeader_match).terminal());
		unitUpdate.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		unitUpdate.addChild(new SuggestionNode(Messages.P2InfHeader_range).terminal());
		unitUpdate.addChild(new SuggestionNode(Messages.P2InfHeader_severity).terminal());
		unitUpdate.addChild(new SuggestionNode(Messages.P2InfHeader_description).terminal());
		indexNode.addChild(unitUpdate);

		SuggestionNode artifacts = new SuggestionNode(Messages.P2InfHeader_artifacts);
		SuggestionNode artifactsIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		artifactsIndex.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		artifactsIndex.addChild(new SuggestionNode("version").terminal()); //$NON-NLS-1$
		artifactsIndex.addChild(new SuggestionNode(Messages.P2InfHeader_classifier).terminal());
		artifacts.addChild(artifactsIndex);
		indexNode.addChild(artifacts);

		SuggestionNode licenses = new SuggestionNode(Messages.P2InfHeader_licenses);
		SuggestionNode licensesIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		licensesIndex.addChild(new SuggestionNode(Messages.P2InfHeader_location).terminal());
		licenses.addChild(licensesIndex);
		indexNode.addChild(licenses);

		SuggestionNode unitRequires = new SuggestionNode(Messages.P2InfHeader_requires);
		SuggestionNode unitProvides = new SuggestionNode(Messages.P2InfHeader_provides);
		SuggestionNode unitProperties = new SuggestionNode(Messages.P2InfHeader_properties);
		SuggestionNode unitMetaReq = new SuggestionNode(Messages.P2InfHeader_metaRequirements);
		SuggestionNode unitHostReq = new SuggestionNode(Messages.P2InfHeader_hostRequirements);
		SuggestionNode unitInstructions = new SuggestionNode(Messages.P2InfHeader_instructions);
		indexNode.addChild(unitRequires);
		indexNode.addChild(unitProvides);
		indexNode.addChild(unitProperties);
		indexNode.addChild(unitMetaReq);
		indexNode.addChild(unitHostReq);
		indexNode.addChild(unitInstructions);

		addRequiresParts(unitRequires);
		addProvidesParts(unitProvides);
		addPropertiesParts(unitProperties);
		addMetaRequiresParts(unitMetaReq);
		addHostRequiresParts(unitHostReq);
		addInstructionsParts(unitInstructions);
	}

	/** Adds an indexed sub-tree using the requires attribute set (namespace, name, range, matchExp, greedy, optional, multiple, filter, min, max). */
	private void addRequiresParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		for (String p : REQUIRES_PARTS) {
			indexNode.addChild(new SuggestionNode(p).terminal());
		}
	}

	/** Adds an indexed sub-tree using the metaRequirements attribute set (namespace, name, range, matchExp, greedy, optional, multiple). */
	private void addMetaRequiresParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		for (String p : META_REQUIRES_PARTS) {
			indexNode.addChild(new SuggestionNode(p).terminal());
		}
	}

	/** Adds an indexed sub-tree with provides-specific attributes: namespace, name, version. */
	private void addProvidesParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		indexNode.addChild(new SuggestionNode(Messages.P2InfHeader_namespace).terminal());
		indexNode.addChild(new SuggestionNode(Messages.P2InfHeader_name).terminal());
		indexNode.addChild(new SuggestionNode(Messages.P2InfHeader_version).terminal());
	}

	/** Adds an indexed sub-tree with properties-specific attributes: name, value. */
	private void addPropertiesParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		indexNode.addChild(new SuggestionNode(Messages.P2InfHeader_name).terminal());
		indexNode.addChild(new SuggestionNode(Messages.P2InfHeader_value).terminal());
	}

	/** Adds flat (non-indexed) terminals for update: id, range, severity, description. */
	private void addUpdateParts(SuggestionNode parent) {
		parent.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		parent.addChild(new SuggestionNode(Messages.P2InfHeader_range).terminal());
		parent.addChild(new SuggestionNode(Messages.P2InfHeader_severity).terminal());
		parent.addChild(new SuggestionNode(Messages.P2InfHeader_description).terminal());
	}

	private void addInstructionsParts(SuggestionNode parent) {
		for (String phase : new String[] { "install", "configure", "unconfigure", "uninstall" }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			SuggestionNode phaseNode = new SuggestionNode(phase).terminal();
			phaseNode.addChild(new SuggestionNode("import").terminal()); //$NON-NLS-1$
			parent.addChild(phaseNode);
		}
	}

	/** Adds an indexed sub-tree using the hostRequirements attribute set (namespace, name, range, greedy, optional, multiple). */
	private void addHostRequiresParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		for (String p : HOST_REQUIRES_PARTS) {
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
				if (node.index && node.terminal) {
					// indexed node that is also a leaf: offer both "0" and "0."
					String valueProposal = value + "0"; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(valueProposal, null, valueProposal, lineStart, value.length()));
					String subkeyProposal = value + "0."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(subkeyProposal, null, subkeyProposal, lineStart, value.length()));
				} else if (node.index) {
					String proposalText = value + "0."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
				} else if (node.terminal && !node.children.isEmpty()) {
					// terminal node that also has children: offer both the bare key and "key."
					String bareProposal = value + node.key;
					completions.add(new TypeCompletionProposal(bareProposal, null, bareProposal, lineStart, value.length()));
					String subkeyProposal = value + node.key + "."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(subkeyProposal, null, subkeyProposal, lineStart, value.length()));
				} else if (node.terminal) {
					String proposalText = value + node.key;
					completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
				} else {
					String proposalText = value + node.key + "."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
				}
			}
			return completions.toArray(ICompletionProposal[]::new);
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
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
			if (token.isEmpty()) {
				continue;
			}
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
			if (next == null) {
				return new ArrayList<>();
			}
			current = next;
		}
		return current.children;
	}
}

