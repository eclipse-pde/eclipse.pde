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
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.ui.model.bundle.BasePackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.model.bundle.ManifestHeader;
import org.eclipse.pde.internal.ui.model.bundle.PackageObject;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleManifestChange {
	
	public static Change createMoveToPackageChange(IFile file, ManifestElement element, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;
			
			TextEdit edit = addPackage(
					(ExportPackageHeader)bundle.getManifestHeader(getExportedPackageHeader(bundle)),
					(BundleModel)bundle.getModel(),
					element);
			if (edit != null) {
				TextFileChange change = new TextFileChange("", file); //$NON-NLS-1$
				change.setEdit(edit);
				return change;
			}
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), monitor);
		}
		return null;
	}
	
	public static MoveFromChange createMovePackageChange(IFile file, String packageName, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;
			
			MultiTextEdit multiEdit = new MultiTextEdit();

			ManifestElement export = removePackage(bundle.getManifestHeader(Constants.EXPORT_PACKAGE), 
					packageName, 
					multiEdit);

			ManifestElement provide = removePackage(bundle.getManifestHeader(ICoreConstants.PROVIDE_PACKAGE),
					packageName, 
					multiEdit);

			removePackage(bundle.getManifestHeader(Constants.IMPORT_PACKAGE), 
					packageName, 
					multiEdit);

			if (multiEdit.hasChildren()) {
				MoveFromChange change = new MoveFromChange("", file); //$NON-NLS-1$
				change.setEdit(multiEdit);
				if (export != null)
					change.setMovedElement(export);
				else if (provide != null)
					change.setMovedElement(provide);
				return change;
			}
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), monitor);
		}
		return null;
	}

	
	public static Change createRenameChange(IFile file, IJavaElement element, String newText,
			IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;
			
			MultiTextEdit multiEdit = new MultiTextEdit();
			
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
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), monitor);
		}
		return null;
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

	private static ManifestElement removePackage(ManifestHeader header, String name, MultiTextEdit multi) {
		ManifestElement result = null;
		if (header instanceof BasePackageHeader) {
			BasePackageHeader bHeader = (BasePackageHeader)header;
			Object obj = bHeader.removePackage(name);
			if (obj instanceof PackageObject) {
				if (!bHeader.isEmpty())
					multi.addChild(new ReplaceEdit(header.getOffset(), header.getLength(), bHeader.write()));
				else
					multi.addChild(new DeleteEdit(header.getOffset(), header.getLength()));
				result = ((PackageObject)obj).getManifestElement();
			}
		}
		return result;
	}

	private static TextEdit addPackage(ExportPackageHeader header, BundleModel model, ManifestElement element) {
		if (header != null) {
			if (!header.hasPackage(element.getValue())) {
				header.addPackage(new ExportPackageObject(header, element, getVersionAttribute(header.getBundle())));
				return new ReplaceEdit(header.getOffset(), header.getLength(), header.write());
			}
		} else {
			String ld = TextUtilities.getDefaultLineDelimiter(model.getDocument());
			header = new ExportPackageHeader(
							getExportedPackageHeader(model.getBundle()), 
							"",  //$NON-NLS-1$
							model.getBundle(), 
							ld);
			header.addPackage(new ExportPackageObject(header, element, getVersionAttribute(header.getBundle())));
			return new InsertEdit(model.getDocument().getLength(), header.write()); 
		}
		return null;
	}
	
    private static String getVersionAttribute(IBundle bundle) {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(bundle);
        return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
	
    private static String getExportedPackageHeader(IBundle bundle) {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(bundle);
        return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
    }

	public static Bundle getBundle(IFile file, IProgressMonitor monitor) throws CoreException, MalformedTreeException, BadLocationException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		manager.connect(file.getFullPath(), monitor);
		
		IDocument document = manager.getTextFileBuffer(file.getFullPath()).getDocument();		
		BundleModel model = new BundleModel(document, false);
		model.load();
		return model.isLoaded() ? (Bundle)model.getBundle() : null;		
	}
	
}
