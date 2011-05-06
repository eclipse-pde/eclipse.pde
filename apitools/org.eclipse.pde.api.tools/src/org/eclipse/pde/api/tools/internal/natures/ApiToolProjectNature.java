/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.natures;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;

public class ApiToolProjectNature implements IProjectNature {
	
	IProject project;

	/**
	 * Add the API plugin builder in the project build spec
	 */
	public void configure() throws CoreException {
		addToBuildSpec(ApiPlugin.BUILDER_ID);
	}

	/**
	 * Remove the API plugin builder from the project build spec
	 */
	public void deconfigure() throws CoreException {
		removeFromBuildSpec(ApiPlugin.BUILDER_ID);
	}

	/**
	 * Retrieve the current project
	 * 
	 * @return the current project
	 */
	public IProject getProject() {
		return this.project;
	}

	/**
	 * Sset the current project
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

	/**
	 * Adds a builder to the build spec for the given project.
	 */
	protected void addToBuildSpec(String builderID) throws CoreException {
		IProjectDescription description = this.project.getDescription();
		ICommand[] oldBuildSpec = description.getBuildSpec();
		int oldApiCommandIndex = -1;
		int length = oldBuildSpec.length;
		loop: for (int i = 0; i < length; ++i) {
			if (oldBuildSpec[i].getBuilderName().equals(builderID)) {
				oldApiCommandIndex = i;
				break loop;
			}
		}
		ICommand[] newCommands;

		ICommand newCommand = description.newCommand();
		newCommand.setBuilderName(builderID);

		if (oldApiCommandIndex == -1) {
			// Add a API build spec after all existing builders
			newCommands = new ICommand[length + 1];
			System.arraycopy(oldBuildSpec, 0, newCommands, 0, length);
			newCommands[length] = newCommand;
		} else {
			oldBuildSpec[oldApiCommandIndex] = newCommand;
			newCommands = oldBuildSpec;
		}

		// Commit the spec change into the project
		description.setBuildSpec(newCommands);
		this.project.setDescription(description, null);
	}
	
	/**
	 * Removes the given builder from the build spec for the given project.
	 */
	protected void removeFromBuildSpec(String builderID) throws CoreException {
		IProjectDescription description = this.project.getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				this.project.setDescription(description, null);
				return;
			}
		}
	}
}
