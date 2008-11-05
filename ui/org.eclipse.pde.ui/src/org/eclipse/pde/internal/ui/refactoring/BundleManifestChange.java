/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219852, 250334
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.ArrayList;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.*;
import org.osgi.framework.Constants;

public class BundleManifestChange {

	public static Change createMoveToPackageChange(IFile file, MoveFromChange change, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;

			BundleModel model = (BundleModel) bundle.getModel();
			BundleTextChangeListener listener = new BundleTextChangeListener(model.getDocument());
			bundle.getModel().addModelChangedListener(listener);
			addPackage(bundle, change);
			return createChange(listener, file);
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
		}
		return null;
	}

	public static MoveFromChange createMovePackageChange(IFile file, Object[] elements, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;

			BundleModel model = (BundleModel) bundle.getModel();
			BundleTextChangeListener listener = new BundleTextChangeListener(model.getDocument());
			bundle.getModel().addModelChangedListener(listener);

			ArrayList list = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof IJavaElement) {
					String packageName = ((IJavaElement) elements[i]).getElementName();
					PDEManifestElement export = removePackage(bundle.getManifestHeader(Constants.EXPORT_PACKAGE), packageName);
					if (export != null)
						list.add(export);
				}
			}

			TextEdit[] operations = listener.getTextOperations();
			if (operations.length > 0) {
				MoveFromChange change = new MoveFromChange("", file); //$NON-NLS-1$
				MultiTextEdit edit = new MultiTextEdit();
				edit.addChildren(operations);
				change.setEdit(edit);
				PDEModelUtility.setChangeTextType(change, file);
				if (list.size() > 0)
					change.setMovedElements((PDEManifestElement[]) list.toArray(new PDEManifestElement[list.size()]));
				return change;
			}
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
		}
		return null;
	}

	public static Change createRenameChange(IFile file, Object[] elements, String[] newTexts, IProgressMonitor monitor) throws CoreException {
		try {
			Bundle bundle = getBundle(file, monitor);
			if (bundle == null)
				return null;

			BundleModel model = (BundleModel) bundle.getModel();
			BundleTextChangeListener listener = new BundleTextChangeListener(model.getDocument());
			bundle.getModel().addModelChangedListener(listener);
			boolean localizationRenamed = false;
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];
				String newText = newTexts[i];
				if (element instanceof IFile) {
					String fileName = ((IFile) element).getProjectRelativePath().toString();
					if (!localizationRenamed && fileName.endsWith(".properties")) { //$NON-NLS-1$
						String oldText = fileName.substring(0, fileName.lastIndexOf(".")); //$NON-NLS-1$
						String oldLocalization = bundle.getLocalization();
						if (LocaleUtil.trimLocalization(oldText).equals(oldLocalization)) {
							renameLocalization(bundle, oldText, newText);
							localizationRenamed = true;
						}
					}
				} else if (element instanceof IType) {
					String oldText = ((IType) element).getFullyQualifiedName('$');
					resetHeaderValue(bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), false, oldText, newText);
					resetHeaderValue(bundle.getManifestHeader(ICoreConstants.PLUGIN_CLASS), false, oldText, newText);
				} else if (element instanceof IPackageFragment) {
					String oldText = ((IPackageFragment) element).getElementName();
					resetHeaderValue(bundle.getManifestHeader(Constants.BUNDLE_ACTIVATOR), true, oldText, newText);
					resetHeaderValue(bundle.getManifestHeader(ICoreConstants.PLUGIN_CLASS), true, oldText, newText);
					renamePackage(bundle.getManifestHeader(Constants.EXPORT_PACKAGE), oldText, newText);
					renamePackage(bundle.getManifestHeader(ICoreConstants.PROVIDE_PACKAGE), oldText, newText);
					renamePackage(bundle.getManifestHeader(Constants.IMPORT_PACKAGE), oldText, newText);
				}
			}
			return createChange(listener, file);
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
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
			PDEModelUtility.setChangeTextType(change, file);
			return change;
		}
		return null;
	}

	private static void renameLocalization(Bundle bundle, String oldText, String newText) {
		if (newText.endsWith(".properties")) //$NON-NLS-1$
			bundle.setLocalization(LocaleUtil.trimLocalization(newText));
		else
			bundle.setLocalization(null);
	}

	private static void resetHeaderValue(IManifestHeader header, boolean isPackage, String oldText, String newText) {
		if (header != null) {
			String value = header.getValue();
			if (oldText.equals(value) || isGoodMatch(value, oldText, isPackage)) {
				StringBuffer buffer = new StringBuffer(newText);
				buffer.append(value.substring(oldText.length()));
				header.setValue(buffer.toString());
			}
		}
	}

	private static boolean isGoodMatch(String value, String oldName, boolean isPackage) {
		if (value == null || value.length() <= oldName.length())
			return false;
		boolean goodLengthMatch = isPackage ? value.lastIndexOf('.') <= oldName.length() : value.charAt(oldName.length()) == '$';
		return value.startsWith(oldName) && goodLengthMatch;
	}

	private static void renamePackage(IManifestHeader header, String oldName, String newName) {
		if (header instanceof BasePackageHeader) {
			BasePackageHeader bHeader = (BasePackageHeader) header;
			bHeader.renamePackage(oldName, newName);
		}
	}

	private static PDEManifestElement removePackage(IManifestHeader header, String name) {
		PDEManifestElement result = null;
		if (header instanceof BasePackageHeader) {
			BasePackageHeader bHeader = (BasePackageHeader) header;
			result = ((PackageObject) bHeader.removePackage(name));
		}
		return result;
	}

	private static void addPackage(Bundle bundle, MoveFromChange change) {
		String headerName = getExportedPackageHeader(bundle);
		ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(headerName);
		ManifestElement[] elements = change.getMovedElements();
		for (int i = 0; i < elements.length; i++) {
			if (header != null) {
				if (!header.hasPackage(change.getPackageName(i))) {
					header.addPackage(new ExportPackageObject(header, elements[i], getVersionAttribute(header.getBundle())));
				}
			} else {
				bundle.setHeader(headerName, change.getMovedText(i));
				header = (ExportPackageHeader) bundle.getManifestHeader(headerName);
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
		manager.connect(file.getFullPath(), LocationKind.NORMALIZE, monitor);

		IDocument document = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE).getDocument();
		BundleModel model = new BundleModel(document, false);
		model.load();
		return model.isLoaded() ? (Bundle) model.getBundle() : null;
	}
}
