/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
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

	@Override
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
		SubMonitor subMonitor = SubMonitor.convert(monitor, 2);
		try {
			IBundle bundle = BundleManifestChange.getBundle(manifest, subMonitor.split(1));
			if (bundle != null) {
				BundleTextChangeListener listener = createListener(bundle);
				if (listener != null) {
					IManifestHeader mHeader = bundle.getManifestHeader(fHeaderKey);
					if (mHeader instanceof BundleSymbolicNameHeader) {
						((BundleSymbolicNameHeader) mHeader).setId(fNewValue);
					} else if (mHeader instanceof RequireBundleHeader) {
						RequireBundleHeader header = (RequireBundleHeader) mHeader;
						RequireBundleObject bundles[] = header.getRequiredBundles();
						for (RequireBundleObject requiredBundle : bundles) {
							if (requiredBundle.getId().equals(fOldValue))
								requiredBundle.setId(fNewValue);
						}
					} else if (mHeader instanceof FragmentHostHeader) {
						((FragmentHostHeader) mHeader).setHostId(fNewValue);
					} else if (mHeader instanceof ExportPackageHeader) {
						ExportPackageObject[] packages = ((ExportPackageHeader) mHeader).getPackages();
						for (ExportPackageObject pkg : packages) {
							PackageFriend[] friends = pkg.getFriends();
							for (PackageFriend friend : friends) {
								if (friend.getName().equals(fOldValue)) {
									pkg.removeFriend(friend);
									pkg.addFriend(new PackageFriend(pkg, fNewValue));
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
			FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), LocationKind.NORMALIZE,
					subMonitor.split(1));
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
