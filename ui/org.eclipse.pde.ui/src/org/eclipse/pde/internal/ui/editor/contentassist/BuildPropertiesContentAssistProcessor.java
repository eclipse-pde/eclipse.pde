/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import java.lang.reflect.Field;
import java.util.ArrayList;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

public class BuildPropertiesContentAssistProcessor extends TypePackageCompletionProcessor
		implements ICompletionListener {

	protected PDESourcePage fSourcePage;
	public BuildPropertiesContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assistSessionEnded(ContentAssistEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		// TODO Auto-generated method stub

	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			String value = doc.get(lineStart, offset - lineStart);
			ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
			Field[] properties = IBuildPropertiesConstants.class.getFields();
			for (Field f : properties) {
				String key = f.getName();
				String element = "";
				try {
					element = (String) f.get(key);
				} catch (IllegalAccessException e) {
					continue;
				}
				if (element.regionMatches(true, 0, value, 0, value.length())) {
					TypeCompletionProposal proposal = new TypeCompletionProposal(element, null, element, lineStart,
							value.length());
					completions.add(proposal);
				}
			}
			return completions.toArray(new ICompletionProposal[completions.size()]);
		} catch (BadLocationException e) {
		}
		return null;
	}
}
