package org.eclipse.pde.internal.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.pde.internal.base.model.component.*;

public class ComponentFactory implements IComponentModelFactory {
	private IComponentModel model;

public ComponentFactory(IComponentModel model) {
	this.model = model;
}
public IComponentFragment createFragment() {
	ComponentFragment fragment = new ComponentFragment();
	fragment.model = model;
	fragment.parent = model.getComponent();
	return fragment;
}
public IComponentPlugin createPlugin() {
	ComponentPlugin plugin = new ComponentPlugin();
	plugin.model = model;
	plugin.parent = model.getComponent();
	return plugin;
}
public IComponentURL createURL() {
	ComponentURL url = new ComponentURL();
	url.model = model;
	url.parent = model.getComponent();
	return url;
}
public IComponentURLElement createURLElement(IComponentURL parent, int elementType) {
	ComponentURLElement element = new ComponentURLElement(elementType);
	element.model = model;
	element.parent = parent;
	return element;
}
}
