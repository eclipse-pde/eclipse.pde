package org.eclipse.pde.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
/**
 * A generic model. Classes that implement this
 * interface are expected to be able to:
 * <ul>
 * <li>Load from an input stream
 * <li>Reload (reset, load, fire 'world change')
 * <li>Dispose (clear all the data and reset)
 * <li>Be associated with a resource (optional)
 * </ul>
 * If a model is not created from a workspace
 * resource file, its underlying resource will
 * be <samp>null</samp>.
 */
public interface IModel extends IAdaptable {
	/**
	 * Releases all the data in this model and
	 * clears the state. A disposed model
	 * can be returned to the normal state
	 * by reloading.
	 */
	void dispose();
	/**
	 * Returns a string found in the resource
	 * bundle associated with this model
	 * for the provided key.
	 *
	 * @param key the name to use for bundle lookup
	 * @return the string for the key in the resource bundle,
	 * or the key itself if not found
	 */
	String getResourceString(String key);
	/**
	 * Returns a workspace resource that this model
	 * is created from. Load/reload operations are
	 * not directly connected with the resource
	 * (although they can be). In some cases,
	 * models will load from a buffer (an editor
	 * document) rather than a resource. However,
	 * the buffer will eventually be synced up
	 * with this resource.
	 * <p>With the caveat of stepped loading, 
	 * all other properties of
	 * the underlying resource could
	 * be used directly (path, project etc.).
	 *
	 * @return a workspace resource (file)
	 * that this model is associated with,
	 * or <samp>null</samp> if the model
	 * is not created from a resource.
	 */
	public IResource getUnderlyingResource();
	/**
	 * Tests if this model has been disposed.
	 * Disposed model cannot be used until
	 * it is loaded/reloaded.
	 * @return true if the model has been disposed
	 */
	boolean isDisposed();
	/**
	 * Tests if this model can be modified. Modification
	 * of a model that is not editable will result
	 * in CoreException being thrown.
	 * @return true if this model can be modified
	 */
	boolean isEditable();

	/**
	 * Tests if this model is loaded and can be used.
	 * @return true if the model has been loaded
	 */
	boolean isLoaded();

	/**
	 * Tests if this model is in sync with the storage
	 * object it was loaded from. Models loaded
	 * from resources are in sync if underlying
	 * resources are in sync. Models loaded
	 * from files on the file systems are in sync
	 * if the time stamp matches the model time stamp.
	 * @return true if the model is in sync
	 * with the file system.
	 */
	boolean isInSync();

	/**
	 * Returns the last modification time stamp.
	 * The model itself does not have the time stamp.
	 * It is 'borrowed' from the underlying physical
	 * object.
	 * @return the time stamp of the underlying physical object.
	 */
	long getTimeStamp();
	/**
	 * Loads the model directly from an underlying
	 * resource. This method does nothing if
	 * this model has no underlying resource
	 * or if there is a buffer stage between
	 * the model and the resource.
	 * <p>This method will throw a CoreException
	 * if errors are encountered during
	 * the loading.
	 */
	public void load() throws CoreException;
	/**
	 * Loads the model from the provided input stream.
	 * This method throws a CoreException if
	 * errors are encountered during the loading.
	 * Upon succesful load, 'isLoaded()' should
	 * return <samp>true</samp>.
	 *
	 * @param source an input stream that should
	 * be parsed to load the model
	 * @param outOfSync if true, time stamp will not be
	 * updated to maintain out-of-sync state of the model.
	 */
	public void load(InputStream source, boolean outOfSync) throws CoreException;
	/**
	 * Reload is a version of 'load' operation that has
	 * the following steps:
	 * <ul>
	 * <li>Reset the model
	 * <li>Load the model
	 * <li>Fire "world changed" event
	 * </ul>
	 * Reload operation is used when a model that
	 * is already in use is invalidated by a change
	 * in the underlying buffer or resource.
	 * Since we don't know the extent of the
	 * change, the only safe thing to do is
	 * to reparse the buffer to sync up. The event that is
	 * subsequently fired should be used by listeners to
	 * discard all caches and/or fully refresh views
	 * that shows any portion of the model.
	 * 
	 * @param source an input stream that should
	 * be parsed to load the model.
	 * @param outOfSync if true, time stamp will not be updated
	 * to maintain out-of-sync state of the model.
	 */
	public void reload(InputStream source, boolean outOfSync) throws CoreException;
}