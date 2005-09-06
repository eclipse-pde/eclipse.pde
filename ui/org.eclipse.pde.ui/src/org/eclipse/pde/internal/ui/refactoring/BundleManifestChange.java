/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.model.bundle.BasePackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.osgi.framework.Constants;

public class BundleManifestChange {
	
	public static Change createTypeMoveChange(IFile file, String oldName, String newName, IProgressMonitor monitor) {
		return null;
	}
	
	public static Change createRenameChange(IFile file, IJavaElement element, String newText,
			IProgressMonitor monitor) throws CoreException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());

			IDocument document = buffer.getDocument();

			try {
				BundleModel model = new BundleModel(document, false);
				model.load();
				if (!model.isLoaded())
					return null;

				MultiTextEdit multiEdit = new MultiTextEdit();
				Bundle bundle = (Bundle)model.getBundle();
				
				if (element instanceof IType) {
					String oldText = ((IType)element).getFullyQualifiedName('$');
					resetHeaderValue(bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), 
							oldText, 
							newText,
							multiEdit);
					
					resetHeaderValue(bundle.getManifestHeader("Plugin-Class"),  //$NON-NLS-1$
							oldText, 
							newText,
							multiEdit);
				} else if (element instanceof IPackageFragment) {
					String oldText = element.getElementName();
					
					resetHeaderValue(bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), 
							oldText, 
							newText,
							multiEdit);			

					resetHeaderValue(bundle.getManifestHeader("Plugin-Class"),  //$NON-NLS-1$
							oldText, 
							newText,
							multiEdit);					
					
					renamePackage(bundle.getManifestHeader(Constants.EXPORT_PACKAGE), 
							oldText, 
							newText,
							multiEdit);

					renamePackage(bundle.getManifestHeader(ICoreConstants.PROVIDE_PACKAGE),
							oldText, 
							newText,
							multiEdit);

					renamePackage(bundle.getManifestHeader(Constants.IMPORT_PACKAGE), 
							oldText, 
							newText,
							multiEdit);
				}
				
				if (multiEdit.hasChildren()) {
					TextFileChange change = new TextFileChange("", file); //$NON-NLS-1$
					change.setEdit(multiEdit);
					return change;
				}
			} catch (CoreException e) {
				return null;
			}
			return null;
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}
	}
	
	private static void resetHeaderValue(ManifestHeader header, String oldText, String newText, MultiTextEdit multi) {
		if (header != null) {
			String value = header.getValue();
			if (value != null && value.startsWith(oldText) && value.lastIndexOf('.') <= oldText.length()) {
				StringBuffer buffer = new StringBuffer(newText);
				buffer.append(value.substring(oldText.length()));
				header.setValue(buffer.toString());
				multi.addChild(new ReplaceEdit(header.getOffset(), header.getLength(), header.write()));
			}
		}
	}
	
	private static void renamePackage(ManifestHeader header, String oldName, String newName, MultiTextEdit multi) {
		if (header instanceof BasePackageHeader) {
			BasePackageHeader bHeader = (BasePackageHeader)header;
			if (bHeader.renamePackage(oldName, newName))
				multi.addChild(new ReplaceEdit(header.getOffset(), header.getLength(), bHeader.write()));
		}	
	}
	
}
