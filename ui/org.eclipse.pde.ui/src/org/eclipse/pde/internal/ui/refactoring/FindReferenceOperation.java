/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Constants;

public class FindReferenceOperation implements IWorkspaceRunnable{
	
	private BundleDescription fDesc;
	private String fNewId;
	private Change[] fChanges;
	
	public FindReferenceOperation(BundleDescription desc, String newId) {
		fDesc = desc;
		fNewId = newId;
	}
	
	public void run(IProgressMonitor monitor) throws CoreException {
		ArrayList list = new ArrayList();
		if (fDesc != null) {
			monitor.beginTask("", 2); //$NON-NLS-1$
			findRequireBundleReferences(fDesc, list, new SubProgressMonitor(monitor, 1));
			findFragmentReferences(fDesc, list, new SubProgressMonitor(monitor, 1));
		} 
		monitor.done();
		fChanges = (Change[])list.toArray(new Change[list.size()]); 
	}
	
	public Change[] getChanges() {
		return fChanges;
	}
	
	private void findRequireBundleReferences(BundleDescription desc, ArrayList changes, IProgressMonitor monitor) throws CoreException {
		String oldId = desc.getSymbolicName();
		BundleDescription[] dependents = desc.getDependents();
		monitor.beginTask("", dependents.length); //$NON-NLS-1$
		for (int i = 0; i < dependents.length; i++) {
			BundleSpecification[] requires = dependents[i].getRequiredBundles();
			boolean found = false;
			for (int j = 0; j < requires.length; j++) {
				if (requires[j].getName().equals(oldId)) {
					CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(PluginRegistry.findModel(dependents[i]),
							Constants.REQUIRE_BUNDLE, oldId, fNewId);
					op.run(new SubProgressMonitor(monitor, 1));
					TextFileChange change =	op.getChange();
					if (change != null)
						changes.add(change);
					found = true;
					break;
				}
			}
			if (!found)
				monitor.worked(1);
		}
	}
	
	private void findFragmentReferences(BundleDescription desc, ArrayList changes, IProgressMonitor monitor) throws CoreException {
		BundleDescription[] fragments = desc.getFragments();
		monitor.beginTask("", fragments.length); //$NON-NLS-1$
		String id = desc.getSymbolicName();
		for (int i = 0; i < fragments.length; i++) {
			IPluginModelBase base = PluginRegistry.findModel(fragments[i]);
			if (base instanceof IFragmentModel && id.equals(((IFragmentModel)(base)).getFragment().getPluginId())) {
				CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(base, Constants.FRAGMENT_HOST,
						id, fNewId);
				op.run(new SubProgressMonitor(monitor, 1));
				TextFileChange change = op.getChange();
				if (change != null)
					changes.add(change);
			} else
				monitor.worked(1);
		}
	}

}
