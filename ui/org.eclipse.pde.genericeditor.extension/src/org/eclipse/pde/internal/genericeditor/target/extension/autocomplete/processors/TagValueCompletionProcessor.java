/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetCompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;

/**
 * Class that computes autocompletions for tag content. Example: <pre> &ltnl&gtar^&lt\nl&gt </pre>
 * where ^ is autocomplete call.
 */
public class TagValueCompletionProcessor extends DelegateProcessor {

	private static final Map<String, String[]> tagTextValues = new HashMap<>();
	static {
		tagTextValues.put(ITargetConstants.OS_TAG, Platform.knownOSValues());
		tagTextValues.put(ITargetConstants.WS_TAG, Platform.knownWSValues());
		tagTextValues.put(ITargetConstants.ARCH_TAG, Platform.knownOSArchValues());
		tagTextValues.put(ITargetConstants.NL_TAG, getLocales());
	}

	private String searchTerm;
	private String acKey;
	private int offset;

	public TagValueCompletionProcessor(String searchTerm, String acKey, int offset) {
		this.searchTerm = searchTerm;
		this.offset = offset;
		this.acKey = acKey;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		List<ICompletionProposal> proposals = new ArrayList<>();
		String[] strings = tagTextValues.get(acKey);
		if (strings != null) {
			Arrays.sort(strings);
			for (String string : strings) {
				StyledString displayString = TargetDefinitionContentAssist.getFilteredStyledString(string, searchTerm);
				if (displayString == null || displayString.length() == 0) {
					continue;
				}
				proposals.add(new TargetCompletionProposal(string, string.length(),
						offset - searchTerm.length(), searchTerm.length(),
						displayString));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public static String[] getLocales() {
		Locale[] locales = Locale.getAvailableLocales();
		String[] result = new String[locales.length];
		for (int i = 0; i < locales.length; i++) {
			result[i] = locales[i].toString();
		}
		return result;
	}

}
