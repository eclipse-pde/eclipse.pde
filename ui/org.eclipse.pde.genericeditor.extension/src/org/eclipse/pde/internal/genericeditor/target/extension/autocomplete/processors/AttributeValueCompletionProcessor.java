/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 520004] autocomplete does not respect tag hierarchy
 *                                 - [Bug 531918] filter suggestions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.InstallableUnitProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.Node;
import org.eclipse.pde.internal.genericeditor.target.extension.model.RepositoryCache;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;

/**
 * Class that computes autocompletions for attribute values. Example:
 * <pre> &ltunit id="org.^" </pre> where ^ is autocomplete call.
 *
 */
public class AttributeValueCompletionProcessor extends DelegateProcessor {

	private String searchTerm;
	private String acKey;
	private int offset;

	public AttributeValueCompletionProcessor(String searchTerm, String acKey, int offset) {
		this.searchTerm = searchTerm;
		this.acKey = acKey;
		this.offset = offset;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		Parser parser = Parser.getDefault();
		Node rootNode = parser.getRootNode();
		if (rootNode == null)
			return new ICompletionProposal[] {};
		List<Node> locationsNode = rootNode.getChildNodesByTag(ITargetConstants.LOCATIONS_TAG);
		if (locationsNode == null || locationsNode.size() == 0)
			return new ICompletionProposal[] {};
		Node locationNode = null;
		for (Node u : locationsNode.get(0).getChildNodesByTag(ITargetConstants.LOCATION_TAG)) {
			if ((offset >= u.getOffsetStart()) && (offset < u.getOffsetEnd())) {
				locationNode = u;
				break;
			}
		}
		if (locationNode == null)
			return new ICompletionProposal[] {};
		UnitNode node = null;
		for (Node u : locationNode.getChildNodesByTag(ITargetConstants.UNIT_TAG)) {
			if ((offset >= u.getOffsetStart()) && (offset < u.getOffsetEnd())) {
				node = (UnitNode)u;
				break;
			}
		}
		if (ITargetConstants.UNIT_ID_ATTR.equalsIgnoreCase(acKey)) {
			if (node != null) {
				if (!(node.getParentNode() instanceof LocationNode))
					return getErrorCompletion();
				LocationNode location = (LocationNode) node.getParentNode();
				String repoLocation = location.getRepositoryLocation();
				if (repoLocation == null) {
					return getErrorCompletion();
				}
				RepositoryCache cache = RepositoryCache.getDefault();
				List<UnitNode> units = cache.fetchP2UnitsFromRepo(repoLocation, false);
				return convertToProposals(units);
			}

		}

		if (ITargetConstants.UNIT_VERSION_ATTR.equalsIgnoreCase(acKey)) {
			if (node != null) {
				if (!(node.getParentNode() instanceof LocationNode))
					return getErrorCompletion();
				LocationNode location = (LocationNode) node.getParentNode();
				String repoLocation = location.getRepositoryLocation();
				if (repoLocation == null) {
					return getErrorCompletion();
				}
				RepositoryCache cache = RepositoryCache.getDefault();
				List<UnitNode> repositoryUnits = cache.fetchP2UnitsFromRepo(repoLocation, false);
				List<String> versions = null;
				for (UnitNode unit : repositoryUnits) {
					if (unit.getId().equals(node.getId())) {
						versions = unit.getAvailableVersions();
					}
				}
				return convertToVersionProposals(versions);
			}

		}

		return new ICompletionProposal[] {};
	}

	private ICompletionProposal[] convertToVersionProposals(List<String> versions) {
		List<String> dest = new ArrayList<>();
		dest.addAll(versions);
		if (!versions.contains(ITargetConstants.UNIT_VERSION_ATTR_GENERIC)) {
			dest.add(ITargetConstants.UNIT_VERSION_ATTR_GENERIC);
		}

		List<ICompletionProposal> result = new ArrayList<>();
		for (String version : dest) {
			StyledString displayString = TargetDefinitionContentAssist.getFilteredStyledString(version, searchTerm);
			if (displayString == null || displayString.length() == 0) {
				continue;
			}
			result.add(new InstallableUnitProposal(displayString, offset - searchTerm.length(), searchTerm.length()));
		}
		return result.toArray(new ICompletionProposal[result.size()]);
	}

	private ICompletionProposal[] convertToProposals(List<UnitNode> units) {
		List<ICompletionProposal> result = new ArrayList<>();
		for (UnitNode unit : units) {
			StyledString displayString = TargetDefinitionContentAssist.getFilteredStyledString(unit.getId(),
					searchTerm);
			if (displayString == null || displayString.length() == 0) {
				continue;
			}
			result.add(new InstallableUnitProposal(displayString, offset - searchTerm.length(), searchTerm.length()));
		}
		return result.toArray(new ICompletionProposal[result.size()]);
	}

	private ICompletionProposal[] getErrorCompletion() {

		String replacementString = Messages.AttributeValueCompletionProcessor_RepositoryRequired;
		return new ICompletionProposal[] {
				new CompletionProposal("", offset, 0, 0, null, replacementString, null, null) }; //$NON-NLS-1$
	}

}
