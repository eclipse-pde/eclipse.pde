package org.eclipse.pde.internal.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.IFileEditorInput;

public class FormatManifestOperation implements IRunnableWithProgress {

	private Object[] fObjects;
	
	public FormatManifestOperation(Object[] objects) {
		fObjects = objects;
	}
	
	public void run(IProgressMonitor mon) throws InvocationTargetException, InterruptedException {
		mon.beginTask(PDEUIMessages.FormatManifestOperation_task, fObjects.length);
		for (int i = 0; !mon.isCanceled() && i < fObjects.length; i++) {
			Object obj = fObjects[i];
			if (obj instanceof IFileEditorInput)
				obj = ((IFileEditorInput)obj).getFile();
			if (obj instanceof IFile) {
				mon.subTask(NLS.bind(PDEUIMessages.FormatManifestOperation_subtask, ((IFile)obj).getFullPath().toString()));
				formatManifest((IFile)obj, mon);
			}
			mon.worked(1);
		}
	}

	private static void formatManifest(Bundle bundle) {
		Enumeration headers = bundle.getHeaders().elements();
		while (headers.hasMoreElements())
			((IManifestHeader)headers.nextElement()).update(true);
		BundleModel model = (BundleModel)bundle.getModel();
		model.adjustOffsets(model.getDocument());
	}

	public static void formatManifest(IFile manifestFile, IProgressMonitor mon) {
		ModelModification mod = new ModelModification(manifestFile) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModel) {
					IBundleModel bundleModel = ((IBundlePluginModel)model).getBundleModel();
					if (bundleModel.getBundle() instanceof Bundle)
						formatManifest((Bundle)bundleModel.getBundle());
				}
			}
		};
		try {
			PDEModelUtility.modifyModel(mod, mon);
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}
	
}
