/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.XMLTextChangeListener;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import junit.framework.TestCase;

public abstract class AbstractDSModelTestCase extends TestCase {

	protected static final String LF = "\n"; //$NON-NLS-1$
	protected static final String CR = "\r"; //$NON-NLS-1$
	protected static final String CRLF = CR + LF;

	public static final String COMPONENT_NAME = "sample"; //$NON-NLS-1$

	protected Document fDocument;
	protected DSModel fModel;
	protected IModelTextChangeListener fListener;

	public AbstractDSModelTestCase() {
	}

	@Override
	protected void setUp() throws Exception {
		fDocument = new Document();
	}

	protected void load() {
		load(false);
	}

	protected void load(boolean addListener) {
		try {
			fModel = new DSModel(fDocument, false);
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
		sb.append("<scr:component xmlns:scr=\"http://www.osgi.org/xmlns/scr/v1.1.0\" name=\"" + COMPONENT_NAME + "\">");
		sb.append(newline);
		if (body != null)
			sb.append(body.toString());
		sb.append(newline);
		sb.append("</scr:component>");
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
