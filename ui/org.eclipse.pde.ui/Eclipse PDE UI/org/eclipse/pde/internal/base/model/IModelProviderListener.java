package org.eclipse.pde.internal.base.model;

/**
 * Classes should implement this interface in order to
 * be able to register as model provider listeners.
 * They will be notified about events such as
 * models being added or removed. These changes
 * are typically caused by the changes in the workspace
 * when models are built on top of workspace resources.
 */
public interface IModelProviderListener {
/**
 * Notifies the listener that models have been
 * changed in the model provider.
 *
 * @param event the event that specifies the type of change
 */
public void modelsChanged(IModelProviderEvent event);
}
