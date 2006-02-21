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

import java.util.ArrayList;

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
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.BasePackageHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PDEManifestElement;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class BundleManifestChange {
	
	public static Change createMoveToPackageChange(IFile file, MoveFromChange change, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;

			BundleModel model = (BundleModel)bundle.getModel();
			BundleTextChangeListener listener = new BundleTextChangeListener(model.getDocument());
			bundle.getModel().addModelChangedListener(listener);			
			addPackage(bundle, change);
			return createChange(listener, file);
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), monitor);
		}
		return null;
	}
	
	public static MoveFromChange createMovePackageChange(IFile file, IJavaElement[] elements, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;
			
			BundleModel model = (BundleModel)bundle.getModel();
			BundleTextChangeListener listener = new BundleTextChangeListener(model.getDocument());
			bundle.getModel().addModelChangedListener(listener);
			
			ArrayList list = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				String packageName = elements[i].getElementName();
				PDEManifestElement export = removePackage(bundle.getManifestHeader(Constants.EXPORT_PACKAGE), 
														  		packageName);
				if (export != null)
					list.add(export);
			}

			TextEdit[] operations = listener.getTextOperations();
			if (operations.length > 0) {
				MoveFromChange change = new MoveFromChange("", file); //$NON-NLS-1$
				MultiTextEdit edit = new MultiTextEdit();
				edit.addChildren(operations);
				change.setEdit(edit);
				if (list.size() > 0)
					change.setMovedElements((PDEManifestElement[])list.toArray(new PDEManifestElement[list.size()]));
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

	public static Change createRenameChange(IFile file, IJavaElement[] elements, String[] newTexts,
			IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;
			
			BundleModel model = (BundleModel)bundle.getModel();
			BundleTextChangeListener listener = new BundleTextChangeListener(model.getDocument());
			bundle.getModel().addModelChangedListener(listener);
			for (int i = 0; i < elements.length; i++) {
				IJavaElement element = elements[i];
				String newText = newTexts[i];
				if (element instanceof IType) {
					String oldText = ((IType)element).getFullyQualifiedName('$');
					resetHeaderValue(bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), 
							false,
							oldText, 
							newText);				
					resetHeaderValue(bundle.getManifestHeader(ICoreConstants.PLUGIN_CLASS), 
							false,
							oldText, 
							newText);
				} else if (element instanceof IPackageFragment) {
					String oldText = element.getElementName();				
					resetHeaderValue(bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), 
							true,
							oldText, 
							newText);			
					resetHeaderValue(bundle.getManifestHeader(ICoreConstants.PLUGIN_CLASS),  
							true,
							oldText, 
							newText);									
					renamePackage(bundle.getManifestHeader(Constants.EXPORT_PACKAGE), 
							oldText, 
							newText);
					renamePackage(bundle.getManifestHeader(ICoreConstants.PROVIDE_PACKAGE),
							oldText, 
							newText);
					renamePackage(bundle.getManifestHeader(Constants.IMPORT_PACKAGE), 
							oldText, 
							newText);
				}
			}
			return createChange(listener, file);
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), monitor);
		}
		return null;
	}
	
	private static Change createChange(BundleTextChangeListener listener, IFile file) {
		TextEdit[] operations = listener.getTextOperations();
		if (operations.length > 0) {
			TextFileChange change = new TextFileChange("", file); //$NON-NLS-1$
			MultiTextEdit edit = new MultiTextEdit();
			edit.addChildren(operations);
			change.setEdit(edit);
			return change;
		}
		return null;
	}
	
	private static void resetHeaderValue(IManifestHeader header, boolean isPackage, String oldText, String newText) {
		if (header != null) {
			String value = header.getValue();
			if (isGoodMatch(value, oldText, isPackage)) {
				StringBuffer buffer = new StringBuffer(newText);
				buffer.append(value.substring(oldText.length()));
				header.setValue(buffer.toString());
			}
		}
	}
	
	private static boolean isGoodMatch(String value, String oldName, boolean isPackage) {
		if (value == null || value.length() <= oldName.length())
			return false;
		boolean goodLengthMatch = isPackage 
									? value.lastIndexOf('.') <= oldName.length() 
									: value.charAt(oldName.length()) == '$';
		return value.startsWith(oldName) && goodLengthMatch; 
	}
	
	private static void renamePackage(IManifestHeader header, String oldName, String newName) {
		if (header instanceof BasePackageHeader) {
			BasePackageHeader bHeader = (BasePackageHeader)header;
			bHeader.renamePackage(oldName, newName);
		}	
	}

	private static PDEManifestElement removePackage(IManifestHeader header, String name) {
		PDEManifestElement result = null;
		if (header instanceof BasePackageHeader) {
			BasePackageHeader bHeader = (BasePackageHeader)header;
			result = ((PackageObject)bHeader.removePackage(name));
		}
		return result;
	}

	private static void addPackage(Bundle bundle, MoveFromChange change) {
		String headerName = getExportedPackageHeader(bundle);
		ExportPackageHeader header = (ExportPackageHeader)bundle.getManifestHeader(headerName);
		ManifestElement[] elements = change.getMovedElements();
		for (int i = 0; i < elements.length; i++) {
			if (header != null) {
				if (!header.hasPackage(change.getPackageName(i))) {
					header.addPackage(new ExportPackageObject(header, elements[i], getVersionAttribute(header.getBundle())));
				}
			} else {
				bundle.setHeader(headerName, change.getMovedText(i));
				header = (ExportPackageHeader)bundle.getManifestHeader(headerName);
			}
		}
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
