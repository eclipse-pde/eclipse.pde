package org.eclipse.pde.internal.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.*;

public abstract class BuildModel extends AbstractModel implements IBuildModel {
	protected Build build;
	private BuildModelFactory factory;
	public boolean fragment;

public IBuild getBuild() {
	if (isLoaded()==false) load();
	return build;
}
public IBuildModelFactory getFactory() {
	if (factory==null) factory = new BuildModelFactory(this);
	return factory;
}
public String getInstallLocation() {
	return null;
}
public boolean isFragment() {
	return fragment;
}
public abstract void load();
public void load(InputStream source, boolean outOfSync) {
	Properties properties = new Properties();
	try {
		properties.load(source);
		if (!outOfSync) updateTimeStamp();
	} catch (IOException e) {
		PDEPlugin.logException(e);
		return;
	}
	build = new Build();
	build.setModel(this);
	for (Enumeration names = properties.propertyNames();
		names.hasMoreElements();
		) {
		String name = names.nextElement().toString();
		build.processEntry(name, (String) properties.get(name));
	}
	loaded = true;
}
public void reload(InputStream source, boolean outOfSync) {
	if (build!=null) build.reset();
	else {
		build = new Build();
		build.setModel(this);
	}
	load(source, outOfSync);
	fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
}
public void setFragment(boolean value) {
	fragment = value;
}
}
