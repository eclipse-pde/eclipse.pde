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
 *                                 - [Bug 528706] autocomplete does not respect multiline tags
 *                                 - [Bug 531918] filter completions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors.AttributeNameCompletionProcessor;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors.AttributeValueCompletionProcessor;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors.TagCompletionProcessor;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors.TagValueCompletionProcessor;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;

/**
 *
 * Main content assist class that is used to dispatch the specific content
 * assist types (see COMPLETION_TYPE_* fields). Uses regex to match each type.
 *
 */
public class TargetDefinitionContentAssist implements IContentAssistProcessor {

	private static final String PREVIOUS_TAGS_MATCH = "(\\s*<(.|\\n)*>\\s*)*"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_SEARCH_TERM_MATCH = PREVIOUS_TAGS_MATCH
			.concat("\\s*<\\s*\\w*(\\s*\\w*\\s*=\\s*\".*?\")*\\s+(?<searchTerm>\\w*)"); //$NON-NLS-1$
	private static final String TAG_SEARCH_TERM_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*(?<searchTerm>\\w*)"); //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE_MATCH_REGEXP = PREVIOUS_TAGS_MATCH
			.concat("\\s*<\\s*\\w*(\\s+\\w+\\s*=\\s*\"(.|\\n)*?\")*\\s+\\w+\\s*=\\s*\"[^\"]*"); //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE_ACKEY_MATCH = PREVIOUS_TAGS_MATCH
			.concat("\\s*<\\s*\\w*(\\s+\\w+\\s*=\\s*\".*?\")*\\s+(?<ackey>\\w+)\\s*=\\s*\"[^\"]*"); //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE_SEARCH_TERM_MATCH = PREVIOUS_TAGS_MATCH
			.concat("\\s*<\\s*\\w*(\\s+\\w+\\s*=\\s*\".*?\")*\\s+\\w+\\s*=\\s*\"(?<searchTerm>[^\"]*)"); //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_MATCH_REGEXP = PREVIOUS_TAGS_MATCH
			.concat("\\s*<\\s*\\w*(\\s*\\w+\\s*=\\s*\"(.|\\n)*?\")*\\s+\\w*"); //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_ACKEY_MATCH = PREVIOUS_TAGS_MATCH
			.concat("\\s*<\\s*(?<ackey>\\w*)(\\s*\\w+\\s*=\\s*\".*?\")*\\s+\\w*"); //$NON-NLS-1$
	private static final String TAG_MATCH_REGEXP = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*"); //$NON-NLS-1$
	private static final String TAG_VALUE_MATCH_REGEXP = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w+[^<]*>\\s*\\w*"); //$NON-NLS-1$
	private static final String TAG_VALUE_SEARCH_TERM_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*(?<searchTerm>\\w*)"); //$NON-NLS-1$
	private static final String TAG_VALUE_ACKEY_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<(?<ackey>\\w*).*"); //$NON-NLS-1$

	private static final int COMPLETION_TYPE_TAG = 0;
	private static final int COMPLETION_TYPE_ATTRIBUTE_NAME = 1;
	private static final int COMPLETION_TYPE_ATTRIBUTE_VALUE = 2;
	private static final int COMPLETION_TYPE_HEADER = 4;
	private static final int COMPLETION_TYPE_TAG_VALUE = 5;
	private static final int COMPLETION_TYPE_UNKNOWN = 6;

	private static final Pattern TAG_SEARCH_TERM_PATTERN = Pattern.compile(TAG_SEARCH_TERM_MATCH, Pattern.DOTALL);
	private static final Pattern ATT_NAME_SEARCH_TERM_PATTERN = Pattern.compile(ATTRIBUTE_NAME_SEARCH_TERM_MATCH,
			Pattern.DOTALL);
	private static final Pattern ATTR_NAME_ACKEY_MATCH = Pattern.compile(ATTRIBUTE_NAME_ACKEY_MATCH, Pattern.DOTALL);
	private static final Pattern ATTR_VALUE_SEARCH_TERM_PATTERN = Pattern.compile(ATTRIBUTE_VALUE_SEARCH_TERM_MATCH,
			Pattern.DOTALL);
	private static final Pattern ATTR_VALUE_ACKEY_PATTERN = Pattern.compile(ATTRIBUTE_VALUE_ACKEY_MATCH,
			Pattern.DOTALL);
	private static final Pattern TAG_VALUE_SEARCH_TERM_PATTERN = Pattern.compile(TAG_VALUE_SEARCH_TERM_MATCH,
			Pattern.DOTALL);
	private static final Pattern TAG_VALUE_ACKEY_PATTERN = Pattern.compile(TAG_VALUE_ACKEY_MATCH, Pattern.DOTALL);

	private String searchTerm = ""; //$NON-NLS-1$
	private String acKey;

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		String text = document.get();
		try {
			Parser.getDefault().parse(document);
		} catch (XMLStreamException e) {
			// TODO handle parsing errors
		}

		int completionType = detectCompletionType(document, text, offset);
		if (completionType == COMPLETION_TYPE_UNKNOWN) {
			return new ICompletionProposal[0];
		}

		if (completionType == COMPLETION_TYPE_TAG) {
			TagCompletionProcessor processor = new TagCompletionProcessor(searchTerm, acKey, offset);
			return processor.getCompletionProposals();
		}

		if (completionType == COMPLETION_TYPE_ATTRIBUTE_NAME) {
			AttributeNameCompletionProcessor processor = new AttributeNameCompletionProcessor(searchTerm, acKey, offset,
					text);
			return processor.getCompletionProposals();
		}

		if (completionType == COMPLETION_TYPE_ATTRIBUTE_VALUE) {
			AttributeValueCompletionProcessor processor = new AttributeValueCompletionProcessor(searchTerm, acKey,
					offset);
			return processor.getCompletionProposals();
		}

		if (completionType == COMPLETION_TYPE_TAG_VALUE) {
			TagValueCompletionProcessor processor = new TagValueCompletionProcessor(searchTerm, acKey, offset);
			return processor.getCompletionProposals();
		}

		return new ICompletionProposal[0];
	}

	private int detectCompletionType(IDocument doc, String text, int offset) {

		if (offset == 0) {
			return COMPLETION_TYPE_HEADER;
		}

		try {
			doc.getLineInformationOfOffset(offset);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return COMPLETION_TYPE_UNKNOWN;
		}
		int indexOfLastTagStart = text.lastIndexOf('<', offset - 1);
		String tagText = text.substring(Math.max(0, indexOfLastTagStart), offset);
		if (tagText.matches(TAG_MATCH_REGEXP)) {
			Matcher matcher = TAG_SEARCH_TERM_PATTERN.matcher(tagText);
			matcher.matches();
			searchTerm = matcher.group("searchTerm"); //$NON-NLS-1$
			return COMPLETION_TYPE_TAG;
		}

		if (tagText.matches(ATTRIBUTE_NAME_MATCH_REGEXP)) {
			Matcher matcher = ATT_NAME_SEARCH_TERM_PATTERN.matcher(tagText);
			matcher.matches();
			searchTerm = matcher.group("searchTerm"); //$NON-NLS-1$
			matcher = ATTR_NAME_ACKEY_MATCH.matcher(tagText);
			matcher.matches();
			acKey = matcher.group("ackey"); //$NON-NLS-1$
			return COMPLETION_TYPE_ATTRIBUTE_NAME;
		}

		if (tagText.matches(ATTRIBUTE_VALUE_MATCH_REGEXP)) {
			Matcher matcher = ATTR_VALUE_SEARCH_TERM_PATTERN.matcher(tagText);
			matcher.matches();
			searchTerm = matcher.group("searchTerm"); //$NON-NLS-1$
			matcher = ATTR_VALUE_ACKEY_PATTERN.matcher(tagText);
			matcher.matches();
			acKey = matcher.group("ackey"); //$NON-NLS-1$
			return COMPLETION_TYPE_ATTRIBUTE_VALUE;
		}

		if (tagText.matches(TAG_VALUE_MATCH_REGEXP)) {
			Matcher matcher = TAG_VALUE_SEARCH_TERM_PATTERN.matcher(tagText);
			matcher.matches();
			searchTerm = matcher.group("searchTerm"); //$NON-NLS-1$
			matcher = TAG_VALUE_ACKEY_PATTERN.matcher(tagText);
			matcher.matches();
			acKey = matcher.group("ackey"); //$NON-NLS-1$
			return COMPLETION_TYPE_TAG_VALUE;
		}

		return COMPLETION_TYPE_UNKNOWN;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '<' };
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	private static Styler bold = new BoldStylerProvider(
			JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT)).getBoldStyler();

	/**
	 * Uses a search term to determine if a string is a match. If it is a match,
	 * then a StyledString is generated showing how it is matched.
	 * 
	 * Matches if searchTerm is empty, string contains searchTerm, or if searchTerm
	 * matches string using the camelCase technique where digits and symbols are
	 * considered as upper case letters
	 * 
	 * @param string
	 *            The string in question
	 * @param searchTerm
	 *            The query string
	 * @return string styled showing how searchTerm is matched, or null if not
	 *         matched
	 */
	public static StyledString getFilteredStyledString(String string, String searchTerm) {
		if (string == null) {
			return null;
		}
		if (searchTerm.isEmpty()) {
			return new StyledString(string);
		} else if (string.toLowerCase().contains(searchTerm.toLowerCase())) {
			int index = string.toLowerCase().indexOf(searchTerm.toLowerCase());
			int len = searchTerm.length();
			StyledString styledString = new StyledString(string.substring(0, index));
			styledString.append(string.substring(index, index + len), bold);
			return styledString.append(string.substring(index + len, string.length()));
		}
		int searchCharIndex = 0;
		int subStringCharIndex = 0;
		String[] stringParts = string.split("((?=[A-Z])|(?<=[._])|(?=[0-9])(?<![0-9]))");
		StyledString styledString = new StyledString();
		while (searchCharIndex < searchTerm.length()) {
			for (String subString : stringParts) {
				if (searchCharIndex == searchTerm.length()) {
					styledString.append(subString);
					continue;
				}
				while (searchCharIndex < searchTerm.length() && subStringCharIndex < subString.length()) {
					if (subString.charAt(subStringCharIndex) == searchTerm.charAt(searchCharIndex)) {
						searchCharIndex++;
						subStringCharIndex++;
					} else {
						break;
					}
				}
				if (subStringCharIndex > 0) {
					styledString.append(subString.substring(0, subStringCharIndex), bold);
					styledString.append(subString.substring(subStringCharIndex));
					subStringCharIndex = 0;
				} else {
					styledString.append(subString);
				}
			}
			if (searchCharIndex == searchTerm.length()) {
				// All of searchTerm has matched in the string
				return styledString;
			} else if (stringParts.length == 0) {
				// Have gone through all substrings without match
				return null;
			} else {
				// Try again looking beyond first substring
				searchCharIndex = 0;
				subStringCharIndex = 0;
				stringParts = Arrays.copyOfRange(stringParts, 1, stringParts.length);
				styledString = new StyledString();
			}
		}
		return null;
	}
}
