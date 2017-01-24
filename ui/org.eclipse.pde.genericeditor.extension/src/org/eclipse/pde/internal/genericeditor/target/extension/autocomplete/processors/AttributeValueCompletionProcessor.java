/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.VersionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.RepositoryCache;
import org.eclipse.pde.internal.genericeditor.target.extension.model.TargetNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;
import org.eclipse.pde.internal.genericeditor.target.extension.p2.UpdateJob;

/**
 * Class that computes autocompletions for attribute values. Example:
 * <pre> &ltunit id="org.^" </pre> where ^ is autocomplete call.
 *
 */
public class AttributeValueCompletionProcessor extends DelegateProcessor {

	private String prefix;
	private String acKey;
	private int offset;

	public AttributeValueCompletionProcessor(String prefix, String acKey, int offset) {
		this.prefix = prefix;
		this.acKey = acKey;
		this.offset = offset;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		Parser parser = Parser.getDefault();
		TargetNode rootNode = parser.getRootNode();
		if (rootNode == null)
			return new ICompletionProposal[] {};

		UnitNode node = rootNode.getEnclosingUnit(offset);

		if (acKey.equalsIgnoreCase(ITargetConstants.UNIT_ID_ATTR)) {
			if (node != null) {
				LocationNode location = node.getParent();
				String repoLocation = location.getRepositoryLocation();
				if (repoLocation == null) {
					return getErrorCompletion();
				}
				RepositoryCache cache = RepositoryCache.getDefault();
				if (!cache.isUpToDate(repoLocation)) {
					scheduleUpdateJob(location);
					return getInformativeProposal();
				}
				List<UnitNode> units = cache.getUnitsByPrefix(repoLocation, prefix);
				return convertToProposals(units);
			}

		}

		boolean replace = false;
		if (acKey.equalsIgnoreCase(ITargetConstants.UNIT_VERSION_ATTR)) {
			if (node != null) {
				LocationNode location = node.getParent();
				String repoLocation = location.getRepositoryLocation();
				if (repoLocation == null) {
					return getErrorCompletion();
				}
				RepositoryCache cache = RepositoryCache.getDefault();
				if (!cache.isUpToDate(repoLocation)) {
					scheduleUpdateJob(location);
					return getInformativeProposal();
				}
				List<UnitNode> byPrefix = cache.getUnitsByPrefix(repoLocation, node.getId());
				List<String> versions = byPrefix.get(0).getAvailableVersions();
				replace = !("".equals(node.getVersion()));//$NON-NLS-1$
				return convertToVersionProposals(versions, replace);
			}

		}

		return new ICompletionProposal[] {};
	}

	private void scheduleUpdateJob(LocationNode location) {
		UpdateJob job = new UpdateJob(location);
		job.setUser(true);
		job.schedule();
	}

	private ICompletionProposal[] convertToVersionProposals(List<String> versions, boolean replace) {
		List<String> dest = new ArrayList<>();
		dest.addAll(versions);
		if (!versions.contains(ITargetConstants.UNIT_VERSION_ATTR_GENERIC)) {
			dest.add(ITargetConstants.UNIT_VERSION_ATTR_GENERIC);
		}

		List<ICompletionProposal> result = new ArrayList<>();
		for (String version : dest) {
			if (version == null) {
				continue;
			}
			if (!version.startsWith(prefix)){
				continue;
			}

			ICompletionProposal proposal = new VersionProposal(version, offset, prefix.length(),
					replace);
			result.add(proposal);
		}
		return result.toArray(new ICompletionProposal[result.size()]);
	}

	private ICompletionProposal[] convertToProposals(List<UnitNode> units) {
		List<ICompletionProposal> result = new ArrayList<>();
		for (UnitNode unit : units) {
			CompletionProposal proposal = new CompletionProposal(unit.getId(), offset - prefix.length(),
					prefix.length(), unit.getId().length() + 1);
			result.add(proposal);
		}
		return result.toArray(new ICompletionProposal[result.size()]);
	}

	private ICompletionProposal[] getInformativeProposal() {
		String displayString = Messages.AttributeValueCompletionProcessor_StartedJob;
		CompletionProposal p = new CompletionProposal("", offset, 0, 0, null, //$NON-NLS-1$
				displayString, null, null);
		return new ICompletionProposal[] { p };
	}

	private ICompletionProposal[] getErrorCompletion() {

		String replacementString = Messages.AttributeValueCompletionProcessor_RepositoryRequired;
		return new ICompletionProposal[] {
				new CompletionProposal("", offset, 0, 0, null, replacementString, null, null) }; //$NON-NLS-1$
	}

}
