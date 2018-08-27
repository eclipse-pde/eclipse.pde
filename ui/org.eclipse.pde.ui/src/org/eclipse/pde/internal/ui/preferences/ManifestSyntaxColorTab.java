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
import org.eclipse.pde.internal.ui.editor.context.ManifestDocumentSetupParticipant;
import org.eclipse.pde.internal.ui.editor.text.*;

public class ManifestSyntaxColorTab extends SyntaxColorTab {

	private static final String[][] COLOR_STRINGS = new String[][] { {PDEUIMessages.ManifestSyntaxColorTab_reservedOSGi, IPDEColorConstants.P_HEADER_OSGI}, {PDEUIMessages.ManifestSyntaxColorTab_keys, IPDEColorConstants.P_HEADER_KEY}, {PDEUIMessages.ManifestSyntaxColorTab_assignment, IPDEColorConstants.P_HEADER_ASSIGNMENT}, {PDEUIMessages.ManifestSyntaxColorTab_values, IPDEColorConstants.P_HEADER_VALUE}, {PDEUIMessages.ManifestSyntaxColorTab_attributes, IPDEColorConstants.P_HEADER_ATTRIBUTES}};

	public ManifestSyntaxColorTab(IColorManager manager) {
		super(manager);
	}

	@Override
	protected IDocument getDocument() {
		StringBuilder buffer = new StringBuilder();
		String delimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		buffer.append("Manifest-Version: 1.0"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("Bundle-Name: %name"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("Bundle-SymbolicName: com.example.xyz"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append("Require-Bundle:"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(" org.eclipse.core.runtime;bundle-version=\"3.0.0\","); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(" org.eclipse.ui;resolution:=optional"); //$NON-NLS-1$
		IDocument document = new Document(buffer.toString());
		new ManifestDocumentSetupParticipant().setup(document);
		return document;
	}

	@Override
	protected ChangeAwareSourceViewerConfiguration getSourceViewerConfiguration() {
		return new ManifestConfiguration(fColorManager);
	}

	@Override
	protected String[][] getColorStrings() {
		return COLOR_STRINGS;
	}

}
