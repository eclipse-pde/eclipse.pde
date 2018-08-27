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
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 531918] filter suggestions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Point;

public class InstallableUnitProposal extends TargetCompletionProposal {

	private String completionString;
	private int offset;

	public InstallableUnitProposal(StyledString completionString, int offset, int length) {
		super(completionString.toString(), completionString.length() + length, offset, length, completionString);
		this.completionString = completionString.toString();
		this.offset = offset;
	}

	@Override
	public void apply(IDocument document) {
		try {
			int indexOf = document.get().indexOf('"', offset);
			document.replace(offset, indexOf - offset, "");
			document.replace(offset, 0, completionString);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(offset + completionString.length(), 0);
	}
}
