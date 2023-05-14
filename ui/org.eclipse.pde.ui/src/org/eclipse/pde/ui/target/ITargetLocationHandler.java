/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.target;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;

/**
 * Contributed target locations that want to support extended editing of target
 * locations can implement this interface
 *
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.13
 */
public interface ITargetLocationHandler {

	/**
	 * Status code that can be set on an OK status returned by
	 * {@link #update(ITargetDefinition, TreePath[], IProgressMonitor)} to
	 * indicate that there is no newer version
	 */
	int STATUS_CODE_NO_CHANGE = 101;
	/**
	 * Status code that can be set on an OK status returned by
	 * {@link #update(ITargetDefinition, TreePath[], IProgressMonitor)},
	 * {@link #remove(ITargetDefinition, TreePath[])} or
	 * {@link #toggle(ITargetDefinition, TreePath[])} to indicate that a
	 * complete target refresh is desired
	 */
	int STATUS_FORCE_RELOAD = 102;

	/**
	 * Returns whether this handler can edit the element described by the given
	 * {@link TreePath}
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePath
	 *            the path to be edited
	 * @return whether this editor can edit the given path of child elements
	 */
	default boolean canEdit(ITargetDefinition target, TreePath treePath) {
		return getEditWizard(target, treePath) != null;
	}

	/**
	 * Returns whether this handler can update the element described by the
	 * given {@link TreePath}
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePath
	 *            the path to be checked
	 * @return whether this editor can update the given path of child elements
	 */
	default boolean canUpdate(ITargetDefinition target, TreePath treePath) {
		return false;
	}

	/**
	 * Returns whether this handler can remove the element described by the
	 * given {@link TreePath}
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePath
	 *            the path to be checked
	 * @return whether this editor can remove the given path of child elements
	 */
	default boolean canRemove(ITargetDefinition target, TreePath treePath) {
		return false;
	}

	/**
	 * Returns whether this handler can disable the element described by the
	 * given {@link TreePath}
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePath
	 *            the path to be checked
	 * @return whether this editor can disable the given path of child elements
	 */
	default boolean canDisable(ITargetDefinition target, TreePath treePath) {
		return false;
	}

	/**
	 * Returns whether this handler can enable the element described by the
	 * given {@link TreePath}
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePath
	 *            the path to be checked
	 * @return whether this editor can enable the given path of child elements
	 */
	default boolean canEnable(ITargetDefinition target, TreePath treePath) {
		return false;
	}

	/**
	 * Returns a wizard that will be opened to edit the element described by the
	 * given {@link TreePath} The wizard is responsible for modifying the target
	 * location and/or target. The target definition will be resolved if the
	 * wizard completes successfully.
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePath
	 *            the path to be edited
	 * @return wizard to open for editing the {@link TreePath} or
	 *         <code>null</code> if editing of the element is not possible
	 */
	default IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
		return null;
	}

	/**
	 * Updates the items given in treePath in the given target to the latest
	 * version
	 *
	 * @param target
	 *            the target definition being updated
	 * @param treePaths
	 *            the array of path to be updated
	 * @param monitor
	 *            to report progress of the update operation
	 * @return result of the update, use an OK status with
	 *         {@link #STATUS_CODE_NO_CHANGE} to indicate everything is up to
	 *         date, and {@link #STATUS_FORCE_RELOAD} to force a reload of the
	 *         target platform
	 */
	default IStatus update(ITargetDefinition target, TreePath[] treePaths, IProgressMonitor monitor) {
		return Status.CANCEL_STATUS;
	}

	/**
	 * Called when the given targetLocations in the given target a re 'reloaded'
	 * and the user wants to completely reload any cached state.
	 *
	 * @param target
	 *            the target definition being edited
	 * @param targetLocations
	 *            the locations to reload
	 * @param monitor
	 *            to report progress of the reload operation
	 * @return the result of the reload
	 */
	default IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
		// default does nothing
		return Status.OK_STATUS;
	}

	/**
	 * Called when the user request to remove the given items from the target
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePaths
	 *            the array of path to be removed
	 * @return result of the update, use an OK status with
	 *         {@link #STATUS_FORCE_RELOAD} to force a reload of the target
	 *         platform
	 */
	default IStatus remove(ITargetDefinition target, TreePath[] treePaths) {
		return Status.CANCEL_STATUS;
	}

	/**
	 * Called when the user request to toggle the enabled/disabled state of the
	 * given items from the target
	 *
	 * @param target
	 *            the target definition being edited
	 * @param treePaths
	 *            the array of path to toggle
	 * @return result of the update, use an OK status with
	 *         {@link #STATUS_FORCE_RELOAD} to force a reload of the target
	 *         platform
	 */
	default IStatus toggle(ITargetDefinition target, TreePath[] treePaths) {
		return Status.CANCEL_STATUS;
	}

}
