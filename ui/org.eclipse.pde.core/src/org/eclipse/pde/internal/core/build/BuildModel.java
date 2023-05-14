/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;

public abstract class BuildModel extends AbstractModel implements IBuildModel {

	private static final long serialVersionUID = 1L;

	protected Build fBuild;

	private BuildModelFactory fFactory;

	@Override
	public IBuild getBuild() {
		if (isLoaded() == false) {
			load();
		}
		return fBuild;
	}

	public IBuild getBuild(boolean createIfMissing) {
		if (fBuild == null && createIfMissing) {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		}
		return getBuild();
	}

	@Override
	public IBuildModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new BuildModelFactory(this);
		}
		return fFactory;
	}

	@Override
	public abstract void load();

	@Override
	public void load(InputStream source, boolean outOfSync) {
		Properties properties = new Properties();
		try {
			properties.load(source);
			if (!outOfSync) {
				updateTimeStamp();
			}
		} catch (IOException e) {
			PDECore.logException(e);
			return;
		}
		fBuild = new Build();
		fBuild.setModel(this);
		properties.forEach((name, value) -> fBuild.processEntry(name.toString(), (String) value));
		setLoaded(true);
	}

	@Override
	public void reload(InputStream source, boolean outOfSync) {
		if (fBuild != null) {
			fBuild.reset();
		} else {
			fBuild = new Build();
			fBuild.setModel(this);
		}
		load(source, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
	}
}
