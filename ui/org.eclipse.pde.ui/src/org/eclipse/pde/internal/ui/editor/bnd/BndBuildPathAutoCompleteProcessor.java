/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.bnd;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.bnd.BndDocument;

import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;

public class BndBuildPathAutoCompleteProcessor implements IContentAssistProcessor {

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		if (document != null) {
			try {
				PropertiesLineReader reader = new PropertiesLineReader(new BndDocument(document));
				LineType type;
				while ((type = reader.next()) != LineType.eof) {
					if (type == LineType.entry) {
						String key = reader.key();
						aQute.bnd.properties.IRegion region = reader.region();
						if (matches(region, offset)) {
							Prefix prefix = getPrefix(document, offset);
							if (Constants.BUILDPATH.equals(key)) {
								Value value = getValue(key, reader, document);
								BundleDescription[] bundles = PDECore.getDefault().getModelManager().getState()
										.getState().getBundles();
								String strippedPrefix = prefix.prefix().strip().toLowerCase();
								Comparator<BundleDescription> prefixMatchFirst = Comparator.comparingInt(
										bd -> bd.getSymbolicName().toLowerCase().startsWith(strippedPrefix) ? 0 : 1);
								Comparator<BundleDescription> orderBySymbolicName = Comparator
										.comparing(BundleDescription::getSymbolicName, String.CASE_INSENSITIVE_ORDER);
								ICompletionProposal[] array = Arrays.stream(bundles)
										.filter(bd -> bd.getSymbolicName() != null)
										.filter(bd -> bd.getSymbolicName().toLowerCase().contains(strippedPrefix))
										.sorted(prefixMatchFirst.thenComparing(orderBySymbolicName)).map(bd -> {
											String replacement = buildReplacement(key, value, bd, prefix);
											CompletionProposal proposal = new CompletionProposal(replacement,
													region.getOffset(), region.getLength(), replacement.length(), null,
													bd.getSymbolicName(), null, null);
											// proposal.setName(getCompletionProposalAutoActivationCharacters())
											return proposal;
										}).toArray(ICompletionProposal[]::new);
								return array;
							} else if (Constants.RUNEE.equals(key)) {
								// TODO suggest any known EE from JDT?
							} else if (Constants.INCLUDERESOURCE.equals(key)) {
								// TODO we might want to suggest resources from
								// the current project?
							}
						}
					}
				}
			} catch (Exception e) {
				// can't do anything here then...
				PDECore.log(Status.error("Internal error on autocompletion", e)); //$NON-NLS-1$
			}

		}
		return new ICompletionProposal[0];
	}

	private String buildReplacement(String key, Value value, BundleDescription bundleDescription, Prefix prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(value.terminatingChar());
		String rawValue = value.value();
		String prefixString = prefix.prefix();
		String substring = rawValue.substring(0, rawValue.length() - prefixString.length());
		sb.append(substring);
		int l = prefixString.length();
		for (int i = 0; i < l; i++) {
			char c = prefixString.charAt(i);
			if (Character.isWhitespace(c)) {
				sb.append(c);
			}

		}
		sb.append(bundleDescription.getSymbolicName());
		return sb.toString();
	}

	private Prefix getPrefix(IDocument document, int offset) {
		try {
			StringBuilder sb = new StringBuilder();
			while (offset > 0) {
				char c = document.getChar(offset - 1);
				if (c == ':' || c == '=' || c == ' ' || c == ',' || c == '\\') {
					return new Prefix(c, sb.toString());
				}
				sb.insert(0, c);
				offset--;
			}
		} catch (BadLocationException e) {
		}
		return new Prefix(' ', ""); //$NON-NLS-1$
	}

	private boolean matches(aQute.bnd.properties.IRegion region, int offset) {
		if (offset >= region.getOffset()) {
			return offset <= region.getOffset() + region.getLength();
		}
		return false;
	}

	private Value getValue(String key, PropertiesLineReader reader, IDocument document) throws BadLocationException {
		// due to bug
		// https://github.com/bndtools/bnd/issues/5839 we can't
		// fetch the value easily... by calling reader.value() ...
		aQute.bnd.properties.IRegion region = reader.region();
		String string = document.get(region.getOffset(), region.getLength()).substring(key.length());
		while (string.length() > 0) {
			char c = string.charAt(0);
			string = string.substring(1);
			if (c == ':' || c == '=') {
				return new Value(c, string);
			}
		}
		return new Value(' ', ""); //$NON-NLS-1$
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return new IContextInformation[0];
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[0];
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return new char[0];
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	private static final record Prefix(char terminatingChar, String prefix) {
	}

	private static final record Value(char terminatingChar, String value) {
	}

}
