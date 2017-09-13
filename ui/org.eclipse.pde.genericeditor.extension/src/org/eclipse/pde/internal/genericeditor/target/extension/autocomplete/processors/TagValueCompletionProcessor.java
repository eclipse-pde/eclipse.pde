/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;

/**
 * Class that computes autocompletions for tag content. Example: <pre> &ltnl&gtar^&lt\nl&gt </pre>
 * where ^ is autcomplete call.
 */
public class TagValueCompletionProcessor extends DelegateProcessor {

	private static final Map<String, String[]> tagTextValues = new HashMap<>();
	static {
		tagTextValues.put(ITargetConstants.OS_TAG, Platform.knownOSValues());
		tagTextValues.put(ITargetConstants.WS_TAG, Platform.knownWSValues());
		tagTextValues.put(ITargetConstants.ARCH_TAG, Platform.knownOSArchValues());
		tagTextValues.put(ITargetConstants.NL_TAG, getLocales());
	}

	private String prefix;
	private String acKey;
	private int offset;
	public TagValueCompletionProcessor(String prefix, String acKey, int offset) {
		this.prefix = prefix;
		this.offset = offset;
		this.acKey = acKey;
	}

	@Override
	public ICompletionProposal[] getCompletionProposals() {
		List<ICompletionProposal> proposals = new ArrayList<>();
		String[] strings = tagTextValues.get(acKey);
		if (strings != null) {
			for (String string : strings) {
				if (string == null || string.length() == 0 || !string.startsWith(prefix)) {
					continue;
				}
				proposals.add(new CompletionProposal(string, offset - prefix.length(), prefix.length(),
						string.length() - 1, null, string, null, null));
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
