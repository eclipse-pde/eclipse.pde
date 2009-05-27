/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.editor.AbstractFoldingStructureProvider;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.osgi.framework.Constants;

public class BundleFoldingStructureProvider extends AbstractFoldingStructureProvider {

	private Map fPositionToElement = new HashMap();

	public BundleFoldingStructureProvider(PDESourcePage editor, IEditingModel model) {
		super(editor, model);
	}

	public void addFoldingRegions(Set currentRegions, IEditingModel model) throws BadLocationException {
		IBundle bundle = ((BundleModel) model).getBundle();

		IManifestHeader importPackageHeader = bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
		IManifestHeader exportPackageHeader = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		IManifestHeader requireBundleHeader = bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);

		try {
			addFoldingRegions(currentRegions, importPackageHeader, model.getDocument());
			addFoldingRegions(currentRegions, exportPackageHeader, model.getDocument());
			addFoldingRegions(currentRegions, requireBundleHeader, model.getDocument());
		} catch (BadLocationException e) {
		}

	}

	private void addFoldingRegions(Set regions, IManifestHeader header, IDocument document) throws BadLocationException {
		if (header == null)
			return;
		int startLine = document.getLineOfOffset(header.getOffset());
		int endLine = document.getLineOfOffset(header.getOffset() + header.getLength() - 1);
		if (startLine < endLine) {
			int start = document.getLineOffset(startLine);
			int end = document.getLineOffset(endLine) + document.getLineLength(endLine);
			Position position = new Position(start, end - start);
			regions.add(position);
			fPositionToElement.put(position, header);
		}
	}

}
