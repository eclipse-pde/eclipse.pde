package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IBaseModel;

/**
 * ModelModification class used my the PDEModelUtility
 * Subclass me to create changes to your models.
 *
 */
public abstract class ModelModification {

	private IFile fFile;
	private boolean fIsBundleModel;
	
	/**
	 * 
	 * @param modelFile the basic underlying file for the model you wish to modify.
	 * @param isBundleModel if true a IBundleModel will be searched for in the open editors (vs. a IPluginModelBase) 
	 */
	public ModelModification(IFile modelFile, boolean isBundleModel) {
		fFile = modelFile;
		fIsBundleModel = isBundleModel;
	}
	
	/**
	 * Invoke this using PDEModelUtility.modifyModel(ModelModification modification)
	 * @param model
	 * @throws CoreException
	 */
	protected abstract void modifyModel(IBaseModel model) throws CoreException;
	
	/**
	 * @return file associated with this model modification
	 */
	protected final IFile getFile() {
		return fFile;
	}
	
	/**
	 * 
	 * @return if this model modification is meant for an IBundleModel
	 */
	protected final boolean searchForBundlePlugin() {
		return fIsBundleModel;
	}
	
	/**
	 * Invoke this using PDEModelUtility.modifyModel(ModelModification modification)
	 * @param model
	 */
	protected final void modifyEditorModel(final IBaseModel model) throws CoreException {
		modifyModel(model);
	}
}
