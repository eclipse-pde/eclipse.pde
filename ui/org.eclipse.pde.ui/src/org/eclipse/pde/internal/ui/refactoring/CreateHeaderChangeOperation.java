/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.*;

public class CreateHeaderChangeOperation implements IWorkspaceRunnable {

	private IPluginModelBase fBase;
	private String fHeaderKey;
	private String fOldValue;
	private String fNewValue;
	private TextFileChange fChange;

	public CreateHeaderChangeOperation(IPluginModelBase base, String headerKey, String oldValue, String newValue) {
		fBase = base;
		fHeaderKey = headerKey;
		fOldValue = oldValue;
		fNewValue = newValue;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		try {
			IResource res = fBase.getUnderlyingResource();
			if (res == null)
				return;
			IProject proj = res.getProject();
			IFile file = PDEProject.getManifest(proj);
			if (file.exists())
				fChange = updateBundleHeader(file, monitor);
		} finally {
			monitor.done();
		}
	}

	public TextFileChange getChange() {
		return fChange;
	}

	protected TextFileChange updateBundleHeader(IFile manifest, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 2); //$NON-NLS-1$
		try {
			IBundle bundle = BundleManifestChange.getBundle(manifest, new SubProgressMonitor(monitor, 1));
			if (bundle != null) {
				BundleTextChangeListener listener = createListener(bundle);
				if (listener != null) {
					IManifestHeader mHeader = bundle.getManifestHeader(fHeaderKey);
					if (mHeader instanceof BundleSymbolicNameHeader) {
						((BundleSymbolicNameHeader) mHeader).setId(fNewValue);
					} else if (mHeader instanceof RequireBundleHeader) {
						RequireBundleHeader header = (RequireBundleHeader) mHeader;
						RequireBundleObject bundles[] = header.getRequiredBundles();
						for (int i = 0; i < bundles.length; i++) {
							if (bundles[i].getId().equals(fOldValue))
								bundles[i].setId(fNewValue);
						}
					} else if (mHeader instanceof FragmentHostHeader) {
						((FragmentHostHeader) mHeader).setHostId(fNewValue);
					} else if (mHeader instanceof ExportPackageHeader) {
						ExportPackageObject[] packages = ((ExportPackageHeader) mHeader).getPackages();
						for (int i = 0; i < packages.length; i++) {
							PackageFriend[] friends = packages[i].getFriends();
							for (int j = 0; j < friends.length; j++) {
								if (friends[j].getName().equals(fOldValue)) {
									packages[i].removeFriend(friends[j]);
									packages[i].addFriend(new PackageFriend(packages[i], fNewValue));
								}
							}
						}
					}

					return getTextChange(listener, manifest);
				}
			}
		} catch (MalformedTreeException e) {
		} catch (CoreException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), LocationKind.NORMALIZE, new SubProgressMonitor(monitor, 1));
			monitor.done();
		}
		return null;
	}

	private BundleTextChangeListener createListener(IBundle bundle) {
		if (bundle != null) {
			BundleTextChangeListener listener = new BundleTextChangeListener(((BundleModel) bundle.getModel()).getDocument());
			bundle.getModel().addModelChangedListener(listener);
			return listener;
		}
		return null;
	}

	private TextFileChange getTextChange(BundleTextChangeListener listener, IFile manifest) {
		TextEdit[] edits = listener.getTextOperations();
		if (edits.length == 0)
			return null;
		MultiTextEdit edit = new MultiTextEdit();
		edit.addChildren(edits);
		TextFileChange change = new TextFileChange("", manifest); //$NON-NLS-1$
		change.setEdit(edit);
		PDEModelUtility.setChangeTextType(change, manifest);
		return change;
	}
}
