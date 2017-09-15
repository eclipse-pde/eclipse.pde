/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 522317] Support environment arguments tags in Generic TP editor
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;

/**
 * Class that computes autocompletions for tags. Example: <pre> &ltun^ </pre>
 * where ^ is autcomplete call.
 */
public class TagCompletionProcessor extends DelegateProcessor {

	private String[] tags = new String[] { ITargetConstants.LOCATIONS_TAG, ITargetConstants.LOCATION_TAG,
			ITargetConstants.TARGET_TAG, ITargetConstants.UNIT_TAG, ITargetConstants.REPOSITORY_TAG,
			ITargetConstants.TARGET_JRE_TAG, ITargetConstants.LAUNCHER_ARGS_TAG, ITargetConstants.VM_ARGS_TAG,
			ITargetConstants.PROGRAM_ARGS_TAG, ITargetConstants.ENVIRONMENT_TAG, ITargetConstants.OS_TAG,
			ITargetConstants.WS_TAG, ITargetConstants.ARCH_TAG, ITargetConstants.NL_TAG };

	private String prefix;
	private int offset;
	public TagCompletionProcessor(String prefix, String acKey, int offset) {
		this.prefix = prefix;
		this.offset = offset;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		List<ICompletionProposal> proposals = new ArrayList<>();

		String handyAddition;
		for (int i = 0; i < tags.length; i++) {
			if (!tags[i].startsWith(prefix))
				continue;
			String proposal = ""; //$NON-NLS-1$
			if (tags[i].equalsIgnoreCase(ITargetConstants.UNIT_TAG)
					|| tags[i].equalsIgnoreCase(ITargetConstants.REPOSITORY_TAG)) {
				handyAddition = "/>"; //$NON-NLS-1$
				proposal = tags[i] + handyAddition;
			} else {
				handyAddition = "</" + tags[i] + ">"; //$NON-NLS-1$  //$NON-NLS-2$
				proposal = tags[i] + ">" + handyAddition; //$NON-NLS-1$
			}
			proposals.add(new CompletionProposal(proposal, offset - prefix.length(), prefix.length(),
					proposal.length() - handyAddition.length(), null, tags[i], null, null));
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

}
