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
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleManifestChange {

	public static Change createRenameChange(IFile file, IJavaElement element, String newName,
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
				
				TextEdit edit = null;

				if (element instanceof IType) {
					IType type = (IType)element;
					
					edit = createHeaderTextEdit(document, 
							bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), 
							type, 
							newName);
					if (edit != null)
						multiEdit.addChild(edit);
					
					edit = createHeaderTextEdit(document, 
						bundle.getManifestHeader("Plugin-Class"),  //$NON-NLS-1$
						(IType)element, 
						newName);
					if (edit != null)
						multiEdit.addChild(edit);
				} else if (element instanceof IPackageFragment) {
					IPackageFragment fragment = (IPackageFragment)element;
					
					edit = createHeaderTextEdit(document, 
							bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), 
							fragment, 
							newName);			
					if (edit != null)
						multiEdit.addChild(edit);

					edit = createHeaderTextEdit(document, 
							bundle.getManifestHeader("Plugin-Class"),  //$NON-NLS-1$
							fragment, 
							newName);					
					if (edit != null)
						multiEdit.addChild(edit);
					
					edit = createExportPackageTextEdit(document, 
							bundle.getManifestHeader(Constants.EXPORT_PACKAGE), 
							fragment.getElementName(), 
							newName);
					if (edit != null)
						multiEdit.addChild(edit);

					edit = createExportPackageTextEdit(document, 
							bundle.getManifestHeader(ICoreConstants.PROVIDE_PACKAGE), 
							fragment.getElementName(), 
							newName);
					if (edit != null)
						multiEdit.addChild(edit);

					edit = createImportPackageTextEdit(document, 
							bundle.getManifestHeader(Constants.IMPORT_PACKAGE), 
							fragment.getElementName(), 
							newName);
					if (edit != null)
						multiEdit.addChild(edit);
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
	
	private static TextEdit createHeaderTextEdit(IDocument doc, ManifestHeader header, IType type, String newName) {
		if (header != null) {
			String value = header.getValue();
			String shortName = type.getElementName();
			String oldName = type.getFullyQualifiedName('$');
			if (value != null && value.startsWith(oldName)) {
				StringBuffer buffer = new StringBuffer(header.getName());
				buffer.append(": "); //$NON-NLS-1$
				buffer.append(oldName.substring(0, oldName.length() - shortName.length()));
				buffer.append(newName);
				buffer.append(value.substring(oldName.length()));
				buffer.append(TextUtilities.getDefaultLineDelimiter(doc));
				return new ReplaceEdit(header.getOffset(), header.getLength(), buffer.toString());
			}
		}
		return null;
	}

	private static TextEdit createHeaderTextEdit(IDocument doc, ManifestHeader header, IPackageFragment fragment, String newName) {
		if (header != null) {
			String value = header.getValue();
			String oldName = fragment.getElementName();
			if (value != null && value.startsWith(oldName) && value.lastIndexOf('.') <= oldName.length()) {
				StringBuffer buffer = new StringBuffer(header.getName());
				buffer.append(": "); //$NON-NLS-1$
				buffer.append(newName);
				buffer.append(value.substring(oldName.length()));
				buffer.append(TextUtilities.getDefaultLineDelimiter(doc));
				return new ReplaceEdit(header.getOffset(), header.getLength(), buffer.toString());
			}
		}
		return null;
	}
	
	private static TextEdit createExportPackageTextEdit(IDocument doc, ManifestHeader header, String oldName, String newName) {
		if (header != null) {
			ExportPackageHeader exportHeader = 
				new ExportPackageHeader(
						header.getName(), 
						header.getValue(),
						header.getBundle(),
						TextUtilities.getDefaultLineDelimiter(doc));
			if (exportHeader.hasPackage(oldName)) {
				exportHeader.renamePackage(oldName, newName);
				return new ReplaceEdit(header.getOffset(), header.getLength(), exportHeader.write());
			}
		}
		return null;
	}

	private static TextEdit createImportPackageTextEdit(IDocument doc, ManifestHeader header, String oldName, String newName) {
		if (header != null) {
			ImportPackageHeader importHeader = 
				new ImportPackageHeader(
						header.getName(), 
						header.getValue(),
						header.getBundle(),
						TextUtilities.getDefaultLineDelimiter(doc));
			if (importHeader.hasPackage(oldName)) {
				importHeader.renamePackage(oldName, newName);
				return new ReplaceEdit(header.getOffset(), header.getLength(), importHeader.write());
			}
		}
		return null;
	}

}
