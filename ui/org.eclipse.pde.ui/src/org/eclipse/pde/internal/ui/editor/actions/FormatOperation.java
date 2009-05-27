/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.plugin.PluginBaseNode;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.ui.IFileEditorInput;

public class FormatOperation implements IRunnableWithProgress {

	private Object[] fObjects;

	public FormatOperation(Object[] objects) {
		fObjects = objects;
	}

	public void run(IProgressMonitor mon) throws InvocationTargetException, InterruptedException {
		mon.beginTask(PDEUIMessages.FormatManifestOperation_task, fObjects.length);
		for (int i = 0; !mon.isCanceled() && i < fObjects.length; i++) {
			Object obj = fObjects[i];
			if (obj instanceof IFileEditorInput)
				obj = ((IFileEditorInput) obj).getFile();
			if (obj instanceof IFile) {
				mon.subTask(NLS.bind(PDEUIMessages.FormatManifestOperation_subtask, ((IFile) obj).getFullPath().toString()));
				format((IFile) obj, mon);
			}
			mon.worked(1);
		}
	}

	public static void format(IFile file, IProgressMonitor mon) {
		PDEModelUtility.modifyModel(new ModelModification(file) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					IBundleModel bundleModel = ((IBundlePluginModelBase) model).getBundleModel();
					if (bundleModel.getBundle() instanceof Bundle)
						formatBundle((Bundle) bundleModel.getBundle());
				} else if (model instanceof IPluginModelBase) {
					IPluginBase pluginModel = ((IPluginModelBase) model).getPluginBase();
					if (pluginModel instanceof PluginBaseNode)
						formatXML((PluginBaseNode) pluginModel);
				}
			}

			public boolean saveOpenEditor() {
				return false;
			}
		}, mon);
	}

	private static void formatBundle(Bundle bundle) {
		Iterator headers = bundle.getHeaders().values().iterator();
		while (headers.hasNext())
			((IManifestHeader) headers.next()).update(true);
		BundleModel model = (BundleModel) bundle.getModel();
		model.adjustOffsets(model.getDocument());
	}

	private static void formatXML(PluginBaseNode node) {
		// TODO Auto-generated method stub

	}
}
