package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.pde.internal.base.model.feature.*;

public class FeatureFactory implements IFeatureModelFactory {
	private IFeatureModel model;

public FeatureFactory(IFeatureModel model) {
	this.model = model;
}

public IFeaturePlugin createPlugin() {
	FeaturePlugin plugin = new FeaturePlugin();
	plugin.model = model;
	plugin.parent = model.getFeature();
	return plugin;
}
public IFeatureURL createURL() {
	FeatureURL url = new FeatureURL();
	url.model = model;
	url.parent = model.getFeature();
	return url;
}

public IFeatureInfo createInfo(String tag) {
	FeatureInfo info = new FeatureInfo(tag);
	info.model = model;
	info.parent = model.getFeature();
	return info;
}

public IFeatureURLElement createURLElement(IFeatureURL parent, int elementType) {
	FeatureURLElement element = new FeatureURLElement(elementType);
	element.model = model;
	element.parent = parent;
	return element;
}
}
