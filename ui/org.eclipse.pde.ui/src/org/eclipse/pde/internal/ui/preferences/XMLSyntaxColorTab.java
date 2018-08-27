/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.pde.internal.ui.editor.text.*;

public class XMLSyntaxColorTab extends SyntaxColorTab {

	private static final String[][] COLOR_STRINGS = new String[][] {
	/*		{Display name, IPreferenceStore key}		*/
	{PDEUIMessages.EditorPreferencePage_text, IPDEColorConstants.P_DEFAULT}, {PDEUIMessages.EditorPreferencePage_proc, IPDEColorConstants.P_PROC_INSTR}, {PDEUIMessages.EditorPreferencePage_tag, IPDEColorConstants.P_TAG}, {PDEUIMessages.EditorPreferencePage_string, IPDEColorConstants.P_STRING}, {PDEUIMessages.XMLSyntaxColorTab_externalizedStrings, IPDEColorConstants.P_EXTERNALIZED_STRING}, {PDEUIMessages.EditorPreferencePage_comment, IPDEColorConstants.P_XML_COMMENT}};

	public XMLSyntaxColorTab(IColorManager manager) {
		super(manager);
	}

	@Override
	protected IDocument getDocument() {
		StringBuilder buffer = new StringBuilder();
		String delimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("<plugin>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("<!-- Comment -->"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("   <extension point=\"some.id\">"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("      <tag name=\"%externalized\">body text</tag>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("   </extension>"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("</plugin>"); //$NON-NLS-1$

		IDocument document = new Document(buffer.toString());
		new XMLDocumentSetupParticpant().setup(document);
		return document;
	}

	@Override
	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return new XMLConfiguration(fColorManager);
	}

	@Override
	protected String[][] getColorStrings() {
		return COLOR_STRINGS;
	}
}
