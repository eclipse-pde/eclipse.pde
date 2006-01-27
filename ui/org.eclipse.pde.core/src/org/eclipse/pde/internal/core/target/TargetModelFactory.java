/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;

public class TargetModelFactory implements ITargetModelFactory {
	
	private ITargetModel fModel;

	public TargetModelFactory(ITargetModel model) {
		fModel = model;
	}

	public ITarget createTarget() {
		return new Target(fModel);
	}

	public IArgumentsInfo createArguments() {
		return new ArgumentsInfo(fModel);
	}

	public IEnvironmentInfo createEnvironment() {
		return new EnvironmentInfo(fModel);
	}

	public ITargetJRE createJREInfo() {
		return new TargetJRE(fModel);
	}

	public ILocationInfo createLocation() {
		return new LocationInfo(fModel);
	}
	
	public IImplicitDependenciesInfo createImplicitPluginInfo() {
		return new ImplicitDependenciesInfo(fModel);
	}

	public ITargetPlugin createPlugin() {
		return new TargetPlugin(fModel);
	}

	public ITargetFeature createFeature() {
		return new TargetFeature(fModel);
	}
	
	public IAdditionalLocation createAdditionalLocation() {
		return new AdditionalDirectory(fModel);
	}

}
