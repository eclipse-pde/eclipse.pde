/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.tests.cheatsheet;

import static org.junit.Assert.fail;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.text.SimpleCSModel;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.junit.Before;

public abstract class AbstractCheatSheetModelTestCase {

	protected static final String LF = "\n"; //$NON-NLS-1$
	protected static final String CR = "\r"; //$NON-NLS-1$
	protected static final String CRLF = CR + LF;

	protected IDocument fDocument;
	protected SimpleCSModel fModel;
	protected IModelTextChangeListener fListener;

	@Before
	public void setUp() {
		fDocument = new Document();
	}

	protected void load() {
		load(false);
	}

	protected void load(boolean addListener) {
		try {
			fModel = new SimpleCSModel(fDocument, false);
			fModel.load();
			if (!fModel.isLoaded() || !fModel.isValid())
				fail("model cannot be loaded");
			if (addListener) {
				fListener = new XMLTextChangeListener(fModel.getDocument());
				fModel.addModelChangedListener(fListener);
			}
		} catch (CoreException e) {
			fail("model cannot be loaded");
		}
	}

	protected void setXMLContents(StringBuilder body, String newline) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append(newline);
		sb.append("<cheatsheet title=\"sample cheatsheet\">");
		sb.append(newline);
		if (body != null)
			sb.append(body.toString());
		sb.append(newline);
		sb.append("</cheatsheet>");
		sb.append(newline);
		fDocument.set(sb.toString());
	}

	protected void reload() {
		TextEdit[] ops = fListener.getTextOperations();
		if (ops.length == 0)
			return;
		MultiTextEdit multi = new MultiTextEdit();
		multi.addChildren(ops);
		try {
			multi.apply(fDocument);
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
			fail(e.getMessage());
		}
		load();
	}
}
