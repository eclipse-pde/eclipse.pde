package org.eclipse.pde.internal.model.jars;

import org.eclipse.core.runtime.*;
import java.util.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.*;

public abstract class JarsModel extends AbstractModel implements IJarsModel {
	protected Jars jars;
	private JarsModelFactory factory;
	public boolean fragment;

public IJarsModelFactory getFactory() {
	if (factory==null) factory = new JarsModelFactory(this);
	return factory;
}
public String getInstallLocation() {
	return null;
}
public IJars getJars() {
	if (isLoaded()==false) load();
	return jars;
}
public boolean isFragment() {
	return fragment;
}
public abstract void load();
public void load(InputStream source) {
	Properties properties = new Properties();
	try {
		properties.load(source);
	} catch (IOException e) {
		PDEPlugin.logException(e);
		return;
	}
	jars = new Jars();
	jars.setModel(this);
	for (Enumeration names = properties.propertyNames();
		names.hasMoreElements();
		) {
		String name = names.nextElement().toString();
		jars.processEntry(name, (String) properties.get(name));
	}
	loaded = true;
}
public void reload(InputStream source) {
	if (jars!=null) jars.reset();
	else {
		jars = new Jars();
		jars.setModel(this);
	}
	load(source);
	fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
}
public void setFragment(boolean value) {
	fragment = value;
}
}
