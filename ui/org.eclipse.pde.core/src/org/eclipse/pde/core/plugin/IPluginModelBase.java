package org.eclipse.pde.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.build.IBuildModel;
/**
 * This type of model is created by parsing the manifest file.
 * It serves as a base interface for both plug-in and
 * fragment models by holding data common to both.
 * If the file is a workspace resource, it will be
 * available as the underlying resource of the model.
 * The model may be read-only or editable.
 * It will also make a reference to the build.properties
 * model when created. The reference will be of the
 * same type as the model itself: if the model is
 * editable, it will attempt to obtain an exclusive
 * editable copy of build.properties model.
 * <p>
 * The plug-in model can be disabled. Disabling the
 * model will not change its data. Users of the
 * model will have to decide if the disabled state
 * if of any importance to them or not.
 * <p>
 * The model is capable of notifying listeners
 * about changes. An attempt to change a read-only
 * model will result in a CoreException.
 */
public interface IPluginModelBase extends IModel, IModelChangeProvider {
	/**
	 * @return org.eclipse.pde.ui.model.plugin.IPluginBase
	 */
	IPluginBase createPluginBase();
	/**
	 * Returns an associated build.properties model
	 * that works in conjunction with this model.
	 *
	 * @return the matching plugin.jars model
	 */
	IBuildModel getBuildModel();
	/**
	 * Returns a factory object that should be used
	 * to create new instances of the model objects.
	 */
	IPluginModelFactory getFactory();
	/**
	 * Returns a location of the file that was used
	 * to create this model. This property is used
	 * only for external models.
	 *
	 * @return a location of the external model, or
	 * <samp>null</samp> if the model is created
	 * from a resource.
	 */
	public String getInstallLocation();
	/**
	 * Returns a top-level model object. Equivalent to
	 * calling <pre>getPluginBase(false)</pre>.
	 * @return a top-level model object representing a plug-in or a fragment.
	 */
	IPluginBase getPluginBase();
	/**
	 * Returns a top-level model object.
	 * @param createIfMissing if true, root model object will
	 * be created if not defined.
	 * @return a top-level model object
	 */
	IPluginBase getPluginBase(boolean createIfMissing);
	/**
	 * Returns </samp>true</samp> if this model is currently enabled.
	 *
	 *@return true if the model is enabled
	 */
	public boolean isEnabled();
	/**
	 * @return boolean
	 */
	boolean isFragmentModel();
	/**
	 * Sets the enable state of the model.
	 *
	 * @param enabled the new enable state
	 */
	public void setEnabled(boolean enabled);
}