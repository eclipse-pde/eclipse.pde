/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) 520004, 528706
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;

/**
 * Class that computes autocompletions for attribute names. Example:
 * <pre> &ltunit ver^ </pre> where ^ is autcomplete call.
 *
 */
public class AttributeNameCompletionProcessor extends DelegateProcessor {

	private String prefix;
	private String acKey;
	private int offset;
	private String text;

	private static final String ATTRIBUTE_NAME_FIND = "(?:\\s*(\\w*)\\s*=\\s*\".*?\")";// $NON-NLS-1$
	private static final Pattern ATT_NAME_PATTERN = Pattern.compile(ATTRIBUTE_NAME_FIND);

	private String[] target = new String[] { ITargetConstants.TARGET_NAME_ATTR, ITargetConstants.TARGET_SEQ_NO_ATTR };
	private String[] locations = new String[] {};
	private String[] location = new String[] { ITargetConstants.LOCATION_INCLUDE_PLATFORMS_ATTR,
			ITargetConstants.LOCATION_INCLUDE_CONFIG_PHASE_ATTR, ITargetConstants.LOCATION_INCLUDE_MODE_ATTR,
			ITargetConstants.LOCATION_INCLUDE_SOURCE_ATTR, ITargetConstants.LOCATION_TYPE_ATTR };
	private String[] unit = new String[] { ITargetConstants.UNIT_ID_ATTR, ITargetConstants.UNIT_VERSION_ATTR };
	private String[] repository = new String[] { ITargetConstants.REPOSITORY_LOCATION_ATTR };
	private String[] targetJRE = new String[] { ITargetConstants.TARGET_JRE_PATH_ATTR };
	private Map<String, String[]> completionMap = new HashMap<>();

	public AttributeNameCompletionProcessor(String prefix, String acKey, int offset, String text) {
		this.prefix = prefix;
		this.acKey = acKey;
		this.offset = offset;
		this.text = text;
		init();
	}

	private void init() {
		completionMap.put(ITargetConstants.TARGET_TAG, target);
		completionMap.put(ITargetConstants.LOCATIONS_TAG, locations);
		completionMap.put(ITargetConstants.LOCATION_TAG, location);
		completionMap.put(ITargetConstants.UNIT_TAG, unit);
		completionMap.put(ITargetConstants.REPOSITORY_TAG, repository);
		completionMap.put(ITargetConstants.TARGET_JRE_TAG, targetJRE);
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		List<ICompletionProposal> proposals = new ArrayList<>();
		String[] strings = completionMap.get(acKey);
		if (strings != null) {
			List<String> attributeStrings = getSibblingAttributeNames();
			for (String string : strings) {
				if (!string.startsWith(prefix) || attributeStrings.contains(string)) {
					continue;
				}
				String proposal = string + "=\"\""; //$NON-NLS-1$
				proposals.add(new CompletionProposal(proposal, offset - prefix.length(), prefix.length(),
						proposal.length() - 1, null, string, null, null));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private List<String> getSibblingAttributeNames() {
		int offsetOfStart = text.lastIndexOf('<', offset);
		int offsetOfEnd = text.indexOf('>', offset);
		String tagText = text.substring(offsetOfStart, offsetOfEnd == -1 ? text.length() : offsetOfEnd);
		List<String> attributeStrings = new ArrayList<>();

		Matcher prefixMatcher = ATT_NAME_PATTERN.matcher(tagText);
		while (prefixMatcher.find()) {
			attributeStrings.add(prefixMatcher.group(1));
		}
		return attributeStrings;
	}

}
