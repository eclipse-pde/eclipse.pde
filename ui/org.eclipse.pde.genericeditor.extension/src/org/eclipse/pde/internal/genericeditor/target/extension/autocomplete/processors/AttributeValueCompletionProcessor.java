/*******************************************************************************
 * Copyright (c) 2018, 2024 Red Hat Inc. and others
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
 *     Lucas Bullen (Red Hat Inc.) - [Bug 520004] autocomplete does not respect tag hierarchy
 *                                 - [Bug 531918] filter suggestions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
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
 * 
 * <pre>
 *  &lt;unit id="org.^"
 * </pre>
 * 
 * where ^ is autocomplete call.
 */
public class AttributeValueCompletionProcessor extends DelegateProcessor {

	private final String searchTerm;
	private final String acKey;
	private final int offset;

	public AttributeValueCompletionProcessor(String searchTerm, String acKey, int offset) {
		this.searchTerm = searchTerm;
		this.acKey = acKey;
		this.offset = offset;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		Parser parser = Parser.getDefault();
		Node rootNode = parser.getRootNode();
		if (rootNode == null) {
			return new ICompletionProposal[] {};
		}
		List<Node> locationsNode = rootNode.getChildNodesByTag(ITargetConstants.LOCATIONS_TAG);
		if (locationsNode == null || locationsNode.isEmpty()) {
			return new ICompletionProposal[] {};
		}
		Node locationNode = null;
		for (Node u : locationsNode.get(0).getChildNodesByTag(ITargetConstants.LOCATION_TAG)) {
			if ((offset >= u.getOffsetStart()) && (offset < u.getOffsetEnd())) {
				locationNode = u;
				break;
			}
		}
		if (locationNode == null) {
			return new ICompletionProposal[] {};
		}
		UnitNode node = null;
		for (Node u : locationNode.getChildNodesByTag(ITargetConstants.UNIT_TAG)) {
			if ((offset >= u.getOffsetStart()) && (offset < u.getOffsetEnd())) {
				node = (UnitNode)u;
				break;
			}
		}
		if (ITargetConstants.UNIT_ID_ATTR.equalsIgnoreCase(acKey)) {
			if (node != null) {
				if (!(node.getParentNode() instanceof LocationNode location)) {
					return getErrorCompletion();
				}
				List<String> repoLocations = location.getRepositoryLocations();
				if (repoLocations.isEmpty()) {
					return getErrorCompletion();
				}
				Map<String, List<IVersionedId>> units = RepositoryCache.fetchP2UnitsFromRepos(repoLocations);
				return toProposals(units.keySet().stream());
			}
		}

		if (ITargetConstants.UNIT_VERSION_ATTR.equalsIgnoreCase(acKey)) {
			if (node != null) {
				if (!(node.getParentNode() instanceof LocationNode location)) {
					return getErrorCompletion();
				}
				List<String> repoLocations = location.getRepositoryLocations();
				if (repoLocations.isEmpty()) {
					return getErrorCompletion();
				}
				Map<String, List<IVersionedId>> units = RepositoryCache.fetchP2UnitsFromRepos(repoLocations);
				List<IVersionedId> versions = units.get(node.getId());
				if (versions != null) {
					Stream<String> availableVersions = Stream.concat(
							versions.stream().map(unit -> unit.getVersion().toString()),
							Stream.of(ITargetConstants.UNIT_VERSION_ATTR_GENERIC));
					return toProposals(availableVersions.distinct());
				}
			}
		}

		if (ITargetConstants.REPOSITORY_LOCATION_ATTR.equalsIgnoreCase(acKey)) {
			List<URI> children = RepositoryCache.fetchChildrenOfRepo(searchTerm);
			return toProposals(children.stream().map(URI::toString));
		}

		return new ICompletionProposal[] {};
	}

	private ICompletionProposal[] toProposals(Stream<String> values) {
		return values.map(value -> TargetDefinitionContentAssist.getFilteredStyledString(value, searchTerm))
				.filter(displayString -> displayString != null && !displayString.isEmpty())
				.map(string -> new InstallableUnitProposal(string, offset - searchTerm.length(), searchTerm.length()))
				.toArray(ICompletionProposal[]::new);
	}

	private ICompletionProposal[] getErrorCompletion() {
		String replacementString = Messages.AttributeValueCompletionProcessor_RepositoryRequired;
		return new ICompletionProposal[] {
				new CompletionProposal("", offset, 0, 0, null, replacementString, null, null) }; //$NON-NLS-1$
	}

}
