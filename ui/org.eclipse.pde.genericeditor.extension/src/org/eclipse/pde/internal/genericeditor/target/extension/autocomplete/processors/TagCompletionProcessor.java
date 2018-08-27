/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 522317] Support environment arguments tags in Generic TP editor
 *                                 - [Bug 520004] autocomplete does not respect tag hierarchy
 *                                 - [Bug 531918] filter completions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TagCompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.Node;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;

/**
 * Class that computes autocompletions for tags. Example: <pre> &ltun^ </pre>
 * where ^ is autocomplete call.
 */
public class TagCompletionProcessor extends DelegateProcessor {
	private static final HashMap<String, String[]> tagChildren = new HashMap<>();
	private static final List<Class<? extends Node>> allowedDuplicatesTags = new ArrayList<>();

	static {
		tagChildren.put(null, new String[] { ITargetConstants.TARGET_TAG });
		tagChildren.put(ITargetConstants.TARGET_TAG,
				new String[] { ITargetConstants.LOCATIONS_TAG, ITargetConstants.TARGET_JRE_TAG,
						ITargetConstants.LAUNCHER_ARGS_TAG, ITargetConstants.ENVIRONMENT_TAG });
		tagChildren.put(ITargetConstants.ENVIRONMENT_TAG, new String[] { ITargetConstants.OS_TAG,
				ITargetConstants.WS_TAG, ITargetConstants.ARCH_TAG, ITargetConstants.NL_TAG });
		tagChildren.put(ITargetConstants.LAUNCHER_ARGS_TAG,
				new String[] { ITargetConstants.VM_ARGS_TAG, ITargetConstants.PROGRAM_ARGS_TAG });
		tagChildren.put(ITargetConstants.LOCATIONS_TAG, new String[] { ITargetConstants.LOCATION_IU_COMPLETION_LABEL,
				ITargetConstants.LOCATION_PROFILE_COMPLETION_LABEL, ITargetConstants.LOCATION_DIRECTORY_COMPLETION_LABEL,
				ITargetConstants.LOCATION_FEATURE_COMPLETION_LABEL });
		tagChildren.put(ITargetConstants.LOCATION_TAG,
				new String[] { ITargetConstants.UNIT_TAG, ITargetConstants.REPOSITORY_TAG });

		allowedDuplicatesTags.add(LocationNode.class);
		allowedDuplicatesTags.add(UnitNode.class);
	}

	private String searchTerm;
	private int offset;

	public TagCompletionProcessor(String searchTerm, String acKey, int offset) {
		this.searchTerm = searchTerm;
		this.offset = offset;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		List<ICompletionProposal> proposals = new ArrayList<>();
		String[] tags = null;
		Parser parser = Parser.getDefault();
		Node node = parser.getRootNode();
		List<Node> children = new ArrayList<>();
		if (node == null) {
			tags = tagChildren.get(null);
		} else if (!isOffsetWithinNode(node)) {
			children.add(node);
			tags = tagChildren.get(null);
		} else {
			children = node.getChildNodes();
			while (children != null && isOffsetWithinNode(node)) {
				Node selectedChildNode = null;
				for (Node child : children) {
					if (isOffsetWithinNode(child)) {
						selectedChildNode = child;
						children = selectedChildNode.getChildNodes();
						break;
					}
				}
				if (selectedChildNode == null) {
					break;
				}
				node = selectedChildNode;
				if (!tagChildren.containsKey(selectedChildNode.getNodeTag())) {
					break;
				}
			}
			if (tagChildren.containsKey(node.getNodeTag())) {
				tags = tagChildren.get(node.getNodeTag());
			} else if (node.getParentNode() != null && tagChildren.containsKey(node.getParentNode().getNodeTag())) {
				tags = tagChildren.get(node.getParentNode().getNodeTag());
				children = node.getParentNode().getChildNodes();
			} else {
				tags = tagChildren.get(null);
			}
		}

		List<String> siblingTags = new ArrayList<>();
		if (children != null) {
			for (Node child : children) {
				if (!allowedDuplicatesTags.contains(child.getClass())) {
					siblingTags.add(child.getNodeTag());
				}
			}
		}

		Arrays.sort(tags);

		for (int i = 0; i < tags.length; i++) {
			StyledString displayString = TargetDefinitionContentAssist.getFilteredStyledString(tags[i], searchTerm);
			if (displayString == null || displayString.length() == 0 || siblingTags.contains(tags[i])) {
				continue;
			}
			proposals.add(new TagCompletionProposal(tags[i], offset - searchTerm.length(), searchTerm.length(),
					displayString));
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private boolean isOffsetWithinNode(Node node) {
		return node != null && offset <= node.getOffsetEnd() && offset > node.getOffsetStart();
	}

}
