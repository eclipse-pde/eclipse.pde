package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
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
			String message = PDEPlugin.getResourceString("SynchronizationOperation.noDefiningPlugin"); //$NON-NLS-1$
			throw new InvocationTargetException(createCoreException(message));
		}
		
		if (model.getUnderlyingResource() == null) {
			String message = PDEPlugin.getResourceString("SynchronizationOperation.externalPlugin"); //$NON-NLS-1$
			throw new InvocationTargetException(createCoreException(message));
		}
		
		super.run(monitor);	
	}
	
	private CoreException createCoreException(String message) {
		IStatus status = new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, message, null); //$NON-NLS-1$
		return new CoreException(status);
	}

}
