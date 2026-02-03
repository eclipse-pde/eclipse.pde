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
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeCompletionProposal;
import org.eclipse.pde.internal.ui.editor.contentassist.TypePackageCompletionProcessor;

/**
 * Content-assist processor for {@code p2.inf} files. Suggestions are driven by
 * a statically built key-segment tree that mirrors the p2.inf property-file
 * grammar
 */
public class P2InfContentAssistProcessor extends TypePackageCompletionProcessor {

	private static final String KEY_PROVIDES = "provides"; //$NON-NLS-1$
	private static final String KEY_REQUIRES = "requires"; //$NON-NLS-1$
	private static final String KEY_META_REQUIREMENTS = "metaRequirements"; //$NON-NLS-1$
	private static final String KEY_PROPERTIES = "properties"; //$NON-NLS-1$
	private static final String KEY_UPDATE = "update"; //$NON-NLS-1$
	private static final String KEY_INSTRUCTIONS = "instructions"; //$NON-NLS-1$
	private static final String KEY_IU = "iu"; //$NON-NLS-1$
	private static final String KEY_UNITS = "units"; //$NON-NLS-1$
	private static final String KEY_ARTIFACTS = "artifacts"; //$NON-NLS-1$
	private static final String KEY_CLASSIFIER = "classifier"; //$NON-NLS-1$
	private static final String KEY_COPYRIGHT = "copyright"; //$NON-NLS-1$
	private static final String KEY_DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String KEY_FILTER = "filter"; //$NON-NLS-1$
	private static final String KEY_GREEDY = "greedy"; //$NON-NLS-1$
	private static final String KEY_HOST_REQUIREMENTS = "hostRequirements"; //$NON-NLS-1$
	private static final String KEY_LICENSES = "licenses"; //$NON-NLS-1$
	private static final String KEY_LOCATION = "location"; //$NON-NLS-1$
	private static final String KEY_MATCH = "match"; //$NON-NLS-1$
	private static final String KEY_MATCH_EXP = "matchExp"; //$NON-NLS-1$
	private static final String KEY_MAX = "max"; //$NON-NLS-1$
	private static final String KEY_MIN = "min"; //$NON-NLS-1$
	private static final String KEY_MULTIPLE = "multiple"; //$NON-NLS-1$
	private static final String KEY_NAME = "name"; //$NON-NLS-1$
	private static final String KEY_NAMESPACE = "namespace"; //$NON-NLS-1$
	private static final String KEY_OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String KEY_RANGE = "range"; //$NON-NLS-1$
	private static final String KEY_SEVERITY = "severity"; //$NON-NLS-1$
	private static final String KEY_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$
	private static final String KEY_VALUE = "value"; //$NON-NLS-1$
	private static final String KEY_VERSION = "version"; //$NON-NLS-1$

	protected PDESourcePage fSourcePage;
	private SuggestionNode root;
	private static final String[] REQUIRES_PARTS = { KEY_NAMESPACE, KEY_NAME, KEY_RANGE, KEY_MATCH_EXP, KEY_GREEDY,
			KEY_OPTIONAL, KEY_MULTIPLE, KEY_FILTER, KEY_MIN, KEY_MAX };

	private static final String[] META_REQUIRES_PARTS = { KEY_NAMESPACE, KEY_NAME, KEY_RANGE, KEY_MATCH_EXP,
			KEY_GREEDY, KEY_OPTIONAL, KEY_MULTIPLE };

	private static final String[] HOST_REQUIRES_PARTS = { KEY_NAMESPACE, KEY_NAME, KEY_RANGE, KEY_GREEDY, KEY_OPTIONAL,
			KEY_MULTIPLE };

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

	private void buildSuggestionTree() {
		root = new SuggestionNode("root"); //$NON-NLS-1$

		SuggestionNode provides = new SuggestionNode(KEY_PROVIDES);
		SuggestionNode requires = new SuggestionNode(KEY_REQUIRES);
		SuggestionNode metaReq = new SuggestionNode(KEY_META_REQUIREMENTS);
		SuggestionNode properties = new SuggestionNode(KEY_PROPERTIES);
		SuggestionNode update = new SuggestionNode(KEY_UPDATE);
		SuggestionNode instructions = new SuggestionNode(KEY_INSTRUCTIONS);
		SuggestionNode iu = new SuggestionNode(KEY_IU);
		SuggestionNode units = new SuggestionNode(KEY_UNITS);

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

		SuggestionNode copyright = new SuggestionNode(KEY_COPYRIGHT);
		copyright.addChild(new SuggestionNode(KEY_LOCATION).terminal());
		indexNode.addChild(copyright);

		SuggestionNode touchpoint = new SuggestionNode(KEY_TOUCHPOINT);
		touchpoint.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		touchpoint.addChild(new SuggestionNode("version").terminal()); //$NON-NLS-1$
		indexNode.addChild(touchpoint);

		SuggestionNode unitUpdate = new SuggestionNode(KEY_UPDATE);
		unitUpdate.addChild(new SuggestionNode(KEY_MATCH).terminal());
		unitUpdate.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		unitUpdate.addChild(new SuggestionNode(KEY_RANGE).terminal());
		unitUpdate.addChild(new SuggestionNode(KEY_SEVERITY).terminal());
		unitUpdate.addChild(new SuggestionNode(KEY_DESCRIPTION).terminal());
		indexNode.addChild(unitUpdate);

		SuggestionNode artifacts = new SuggestionNode(KEY_ARTIFACTS);
		SuggestionNode artifactsIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		artifactsIndex.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		artifactsIndex.addChild(new SuggestionNode("version").terminal()); //$NON-NLS-1$
		artifactsIndex.addChild(new SuggestionNode(KEY_CLASSIFIER).terminal());
		artifacts.addChild(artifactsIndex);
		indexNode.addChild(artifacts);

		SuggestionNode licenses = new SuggestionNode(KEY_LICENSES);
		SuggestionNode licensesIndex = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		licensesIndex.addChild(new SuggestionNode(KEY_LOCATION).terminal());
		licenses.addChild(licensesIndex);
		indexNode.addChild(licenses);

		SuggestionNode unitRequires = new SuggestionNode(KEY_REQUIRES);
		SuggestionNode unitProvides = new SuggestionNode(KEY_PROVIDES);
		SuggestionNode unitProperties = new SuggestionNode(KEY_PROPERTIES);
		SuggestionNode unitMetaReq = new SuggestionNode(KEY_META_REQUIREMENTS);
		SuggestionNode unitHostReq = new SuggestionNode(KEY_HOST_REQUIREMENTS);
		SuggestionNode unitInstructions = new SuggestionNode(KEY_INSTRUCTIONS);
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

	private void addRequiresParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		for (String p : REQUIRES_PARTS) {
			indexNode.addChild(new SuggestionNode(p).terminal());
		}
	}

	private void addMetaRequiresParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		for (String p : META_REQUIRES_PARTS) {
			indexNode.addChild(new SuggestionNode(p).terminal());
		}
	}

	private void addProvidesParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		indexNode.addChild(new SuggestionNode(KEY_NAMESPACE).terminal());
		indexNode.addChild(new SuggestionNode(KEY_NAME).terminal());
		indexNode.addChild(new SuggestionNode(KEY_VERSION).terminal());
	}

	private void addPropertiesParts(SuggestionNode parent) {
		SuggestionNode indexNode = new SuggestionNode("{#}").index(); //$NON-NLS-1$
		parent.addChild(indexNode);
		indexNode.addChild(new SuggestionNode(KEY_NAME).terminal());
		indexNode.addChild(new SuggestionNode(KEY_VALUE).terminal());
	}

	private void addUpdateParts(SuggestionNode parent) {
		parent.addChild(new SuggestionNode("id").terminal()); //$NON-NLS-1$
		parent.addChild(new SuggestionNode(KEY_RANGE).terminal());
		parent.addChild(new SuggestionNode(KEY_SEVERITY).terminal());
		parent.addChild(new SuggestionNode(KEY_DESCRIPTION).terminal());
	}

	private void addInstructionsParts(SuggestionNode parent) {
		for (String phase : new String[] { "install", "configure", "unconfigure", "uninstall" }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			SuggestionNode phaseNode = new SuggestionNode(phase).terminal();
			phaseNode.addChild(new SuggestionNode("import").terminal()); //$NON-NLS-1$
			parent.addChild(phaseNode);
		}
	}

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
			String sep = (value.isEmpty() || value.endsWith(".")) ? "" : "."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			for (SuggestionNode node : suggestions) {
				if (node.index && node.terminal) {
					String valueProposal = value + sep + "0"; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(valueProposal, null, valueProposal, lineStart, value.length()));
					String subkeyProposal = value + sep + "0."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(subkeyProposal, null, subkeyProposal, lineStart, value.length()));
				} else if (node.index) {
					String proposalText = value + sep + "0."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
				} else if (node.terminal && !node.children.isEmpty()) {
					String bareProposal = value + sep + node.key;
					completions.add(new TypeCompletionProposal(bareProposal, null, bareProposal, lineStart, value.length()));
					String subkeyProposal = value + sep + node.key + "."; //$NON-NLS-1$
					completions.add(new TypeCompletionProposal(subkeyProposal, null, subkeyProposal, lineStart, value.length()));
				} else if (node.terminal) {
					String proposalText = value + sep + node.key;
					completions.add(new TypeCompletionProposal(proposalText, null, proposalText, lineStart, value.length()));
				} else {
					String proposalText = value + sep + node.key + "."; //$NON-NLS-1$
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

