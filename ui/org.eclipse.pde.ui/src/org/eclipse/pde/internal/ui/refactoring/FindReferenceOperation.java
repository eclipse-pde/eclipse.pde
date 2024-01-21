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

import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.osgi.framework.Constants;
import org.osgi.resource.Resource;

public class FindReferenceOperation implements IWorkspaceRunnable {

	private final BundleDescription fDesc;
	private final String fNewId;
	private Change[] fChanges;

	public FindReferenceOperation(BundleDescription desc, String newId) {
		fDesc = desc;
		fNewId = newId;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		ArrayList<TextFileChange> list = new ArrayList<>();
		if (fDesc != null) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
			findRequireBundleReferences(list, subMonitor.split(1));
			findFragmentReferences(list, subMonitor.split(1));
			findXFriendReferences(list, subMonitor.split(1));
		}
		fChanges = list.toArray(new Change[list.size()]);
	}

	public Change[] getChanges() {
		return fChanges;
	}

	private void findRequireBundleReferences(ArrayList<TextFileChange> changes, IProgressMonitor monitor) throws CoreException {
		String oldId = fDesc.getSymbolicName();
		BundleDescription[] dependents = fDesc.getDependents();
		SubMonitor subMonitor = SubMonitor.convert(monitor, dependents.length);
		for (BundleDescription dependent : dependents) {
			BundleSpecification[] requires = dependent.getRequiredBundles();
			SubMonitor iterationMonitor = subMonitor.split(1);
			for (BundleSpecification require : requires) {
				if (require.getName().equals(oldId)) {
					CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(
							PluginRegistry.findModel((Resource) dependent), Constants.REQUIRE_BUNDLE, oldId, fNewId);
					op.run(iterationMonitor);
					TextFileChange change = op.getChange();
					if (change != null) {
						changes.add(change);
					}
					break;
				}
			}
		}
	}

	private void findFragmentReferences(ArrayList<TextFileChange> changes, IProgressMonitor monitor) throws CoreException {
		BundleDescription[] fragments = fDesc.getFragments();
		SubMonitor subMonitor = SubMonitor.convert(monitor, fragments.length);
		String id = fDesc.getSymbolicName();
		for (Resource fragment : fragments) {
			IPluginModelBase base = PluginRegistry.findModel(fragment);
			SubMonitor iterationMonitor = subMonitor.split(1);
			if (base instanceof IFragmentModel && id.equals(((IFragmentModel) (base)).getFragment().getPluginId())) {
				CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(base, Constants.FRAGMENT_HOST, id, fNewId);
				op.run(iterationMonitor);
				TextFileChange change = op.getChange();
				if (change != null) {
					changes.add(change);
				}
			}
		}
	}

	private void findXFriendReferences(ArrayList<TextFileChange> changes, IProgressMonitor monitor) throws CoreException {
		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		ExportPackageDescription[] pkgs = helper.getVisiblePackages(fDesc);
		String id = fDesc.getSymbolicName();
		SubMonitor subMonitor = SubMonitor.convert(monitor, pkgs.length);
		for (ExportPackageDescription pkg : pkgs) {
			SubMonitor iterationMonitor = subMonitor.split(1);
			String[] friends = (String[]) pkg.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
			if (friends != null)
				for (String friend : friends) {
					if (friend.equals(id)) {
						CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(
								PluginRegistry.findModel((Resource) pkg.getExporter()), Constants.EXPORT_PACKAGE, id,
								fNewId);
						op.run(iterationMonitor);
						TextFileChange change = op.getChange();
						if (change != null)
							changes.add(change);
						break;
					}
				}
		}
	}

}
