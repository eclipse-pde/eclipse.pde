package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.swt.widgets.*;


public class SynchronizationOperation extends ProductDefinitionOperation {

	public SynchronizationOperation(IProduct product, Shell shell) {
		super(product, getPluginId(product), getProductId(product), product.getApplication(), shell);
	}
	
	private static String getProductId(IProduct product) {
		String full = product.getId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(index + 1) : full;
	}
	
	private static String getPluginId(IProduct product) {
		String full = product.getId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(0, index) : full;
	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(fPluginId);
		if (model == null) {
			String message = "The product's defining plug-in could not be found.";
			throw new InvocationTargetException(createCoreException(message));
		}
		
		if (model.getUnderlyingResource() == null) {
			String message = "The product's defining plug-in is not in the workspace and it cannot me modified.  Please import it into your workspace and retry.";
			throw new InvocationTargetException(createCoreException(message));
		}
		
		super.run(monitor);	
	}
	
	private CoreException createCoreException(String message) {
		IStatus status = new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, message, null);
		return new CoreException(status);
	}

}
