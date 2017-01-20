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
package org.eclipse.pde.genericeditor.target.extension.autocomplete;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class VersionProposal implements ICompletionProposal {

	private static final String REPLACE_STRING = " (replace)";//$NON-NLS-1$
	private String completionString;
	private int offset;
	private int length;
	private boolean replace;

	public VersionProposal(String completionString, int offset, int length, boolean replace) {
		this.completionString = completionString;
		this.offset = offset;
		this.length = length;
		this.replace = replace;
	}

	@Override
	public void apply(IDocument document) {
		String toReplace;
		toReplace = completionString.substring(length);

		try {
			if (replace) {
				int indexOf = document.get().indexOf('"', offset);
				document.replace(offset, indexOf - offset, "");
				document.replace(offset, 0, toReplace);
			} else {
				document.replace(offset, 0, toReplace);
			}

		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(offset + completionString.length() - length, 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return replace ? completionString.concat(REPLACE_STRING) : completionString;
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

}
