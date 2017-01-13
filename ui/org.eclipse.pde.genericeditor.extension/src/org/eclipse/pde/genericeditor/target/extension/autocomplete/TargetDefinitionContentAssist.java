/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.target.extension.autocomplete;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.pde.genericeditor.target.extension.autocomplete.processors.AttributeNameCompletionProcessor;
import org.eclipse.pde.genericeditor.target.extension.autocomplete.processors.AttributeValueCompletionProcessor;
import org.eclipse.pde.genericeditor.target.extension.autocomplete.processors.TagCompletionProcessor;
import org.eclipse.pde.genericeditor.target.extension.model.xml.Parser;

/**
 *
 * Main content assist class that is used to dispatch the specific content
 * assist types (see COMPLETION_TYPE_* fields). Uses regex to match each type.
 *
 */
public class TargetDefinitionContentAssist implements IContentAssistProcessor {

	private static final String PREVIOUS_TAGS_MATCH = "(\\s*<.*>\\s*)*"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_PREFIX_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*(\\s*\\w*\\s*=\\s*\".*?\")*\\s+(?<prefix>\\w*)"); //$NON-NLS-1$
	private static final String TAG_PREFIX_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*(?<prefix>\\w*)"); //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE_MATCH_REGEXP = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*(\\s+\\w*\\s*=\\s*\".*?\")*\\s+\\w*\\s*=\\s*\"[^\"]*"); //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE_ACKEY_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*(\\s+\\w*\\s*=\\s*\".*?\")*\\s+(?<ackey>\\w*)\\s*=\\s*\"[^\"]*"); //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE_PREFIX_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*(\\s+\\w*\\s*=\\s*\".*?\")*\\s+\\w*\\s*=\\s*\"(?<prefix>[^\"]*)"); //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_MATCH_REGEXP = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*(\\s*\\w*\\s*=\\s*\".*?\")*\\s+\\w*"); //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_ACKEY_MATCH = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*(?<ackey>\\w*)(\\s*\\w*\\s*=\\s*\".*?\")*\\s+\\w*"); //$NON-NLS-1$
	private static final String TAG_MATCH_REGEXP = PREVIOUS_TAGS_MATCH.concat("\\s*<\\s*\\w*"); //$NON-NLS-1$

	private static final int COMPLETION_TYPE_TAG = 0;
	private static final int COMPLETION_TYPE_ATTRIBUTE_NAME = 1;
	private static final int COMPLETION_TYPE_ATTRIBUTE_VALUE = 2;
	private static final int COMPLETION_TYPE_HEADER = 4;
	private static final int COMPLETION_TYPE_UNKNOWN = 5;

	private static final Pattern TAG_PREFIX_PATTERN = Pattern.compile(TAG_PREFIX_MATCH);
	private static final Pattern ATT_NAME_PREFIX_PATTERN = Pattern.compile(ATTRIBUTE_NAME_PREFIX_MATCH);
	private static final Pattern ATTR_NAME_ACKEY_MATCH = Pattern.compile(ATTRIBUTE_NAME_ACKEY_MATCH);
	private static final Pattern ATTR_VALUE_PREFIX_PATTERN = Pattern.compile(ATTRIBUTE_VALUE_PREFIX_MATCH);
	private static final Pattern ATTR_VALUE_ACKEY_PATTERN = Pattern.compile(ATTRIBUTE_VALUE_ACKEY_MATCH);
	
	private String prefix = ""; //$NON-NLS-1$
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
			TagCompletionProcessor processor = new TagCompletionProcessor(prefix, acKey, offset);
			return processor.getCompletionProposals();
		}

		if (completionType == COMPLETION_TYPE_ATTRIBUTE_NAME) {
			AttributeNameCompletionProcessor processor = new AttributeNameCompletionProcessor(prefix, acKey, offset);
			return processor.getCompletionProposals();
		}

		if (completionType == COMPLETION_TYPE_ATTRIBUTE_VALUE) {
			AttributeValueCompletionProcessor processor = new AttributeValueCompletionProcessor(prefix, acKey, offset);
			return processor.getCompletionProposals();
		}

		return new ICompletionProposal[0];
	}

	private int detectCompletionType(IDocument doc, String text, int offset) {

		if (offset == 0){
			return COMPLETION_TYPE_HEADER;
		}

		IRegion lineInfo = null;
		try {
			lineInfo = doc.getLineInformationOfOffset(offset);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return COMPLETION_TYPE_UNKNOWN;
		}
		String lineText = text.substring(lineInfo.getOffset(), lineInfo.getOffset() + lineInfo.getLength());
		int deltaOffset = offset - lineInfo.getOffset();
		String partialLineText = lineText.substring(0, deltaOffset);
		if (partialLineText.matches(TAG_MATCH_REGEXP)) {
			Matcher matcher = TAG_PREFIX_PATTERN.matcher(partialLineText);
			matcher.matches();
			prefix = matcher.group("prefix"); //$NON-NLS-1$
			return COMPLETION_TYPE_TAG;
		}

		if (partialLineText.matches(ATTRIBUTE_NAME_MATCH_REGEXP)) {
			Matcher matcher = ATT_NAME_PREFIX_PATTERN.matcher(partialLineText);
			matcher.matches();
			prefix = matcher.group("prefix"); //$NON-NLS-1$
			matcher = ATTR_NAME_ACKEY_MATCH.matcher(partialLineText);
			matcher.matches();
			acKey = matcher.group("ackey"); //$NON-NLS-1$
			return COMPLETION_TYPE_ATTRIBUTE_NAME;
		}

		if (partialLineText.matches(ATTRIBUTE_VALUE_MATCH_REGEXP)) {
			Matcher matcher = ATTR_VALUE_PREFIX_PATTERN.matcher(partialLineText);
			matcher.matches();
			prefix = matcher.group("prefix"); //$NON-NLS-1$
			matcher = ATTR_VALUE_ACKEY_PATTERN.matcher(partialLineText);
			matcher.matches();
			acKey = matcher.group("ackey"); //$NON-NLS-1$
			return COMPLETION_TYPE_ATTRIBUTE_VALUE;
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

}
