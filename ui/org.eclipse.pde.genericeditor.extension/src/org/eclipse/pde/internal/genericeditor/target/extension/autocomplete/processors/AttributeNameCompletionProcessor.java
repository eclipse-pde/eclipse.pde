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
 *     Lucas Bullen (Red Hat Inc.) 520004, 528706, 531918
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetCompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;

/**
 * Class that computes autocompletions for attribute names. Example:
 * <pre> &ltunit ver^ </pre> where ^ is autocomplete call.
 *
 */
public class AttributeNameCompletionProcessor extends DelegateProcessor {

	private String searchTerm;
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

	public AttributeNameCompletionProcessor(String searchTerm, String acKey, int offset, String text) {
		this.searchTerm = searchTerm;
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
			Arrays.sort(strings);
			List<String> attributeStrings = getSibblingAttributeNames();
			for (String string : strings) {
				StyledString displayString = TargetDefinitionContentAssist.getFilteredStyledString(string, searchTerm);
				if (displayString == null || displayString.length() == 0 || attributeStrings.contains(string)) {
					continue;
				}
				String proposal = string + "=\"\""; //$NON-NLS-1$
				proposals.add(new TargetCompletionProposal(proposal, proposal.length(),
						offset - searchTerm.length(), searchTerm.length(),
						displayString));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private List<String> getSibblingAttributeNames() {
		int offsetOfStart = text.lastIndexOf('<', offset);
		int offsetOfEnd = text.indexOf('>', offset);
		String tagText = text.substring(offsetOfStart, offsetOfEnd == -1 ? text.length() : offsetOfEnd);
		List<String> attributeStrings = new ArrayList<>();

		Matcher searchTermMatcher = ATT_NAME_PATTERN.matcher(tagText);
		while (searchTermMatcher.find()) {
			attributeStrings.add(searchTermMatcher.group(1));
		}
		return attributeStrings;
	}

}
