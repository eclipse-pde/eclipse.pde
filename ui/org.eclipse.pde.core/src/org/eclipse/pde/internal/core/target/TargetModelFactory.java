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

import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IRuntimeInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;

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
		return new EnvironmnetInfo(fModel);
	}

	public IRuntimeInfo createJREInfo() {
		return new RuntimeInfo(fModel);
	}

}
