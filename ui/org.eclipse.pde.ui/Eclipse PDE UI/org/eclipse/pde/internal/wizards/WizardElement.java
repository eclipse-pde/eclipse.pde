package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.core.runtime.*;


public class WizardElement extends NamedElement {
	public static final String ATT_NAME = "name";
	public static final String TAG_DESCRIPTION = "description";
	public static final String ATT_ICON = "icon";
	public static final String ATT_ID = "id";
	public static final String ATT_CLASS = "class";
	private String          description;
	private IConfigurationElement configurationElement;

public WizardElement(IConfigurationElement config) {
	super(config.getAttribute(ATT_NAME));
	this.configurationElement = config;
}
public Object createExecutableExtension ()  throws CoreException {
	return configurationElement.createExecutableExtension(ATT_CLASS);
}
public IConfigurationElement getConfigurationElement() {
	return configurationElement;
}
public String getDescription() {
	if (description == null) {
		IConfigurationElement[] children = configurationElement.getChildren(TAG_DESCRIPTION);
		if (children.length > 0) {
			description = children[0].getValue();
		}
	}
	return description;
}
public String getID() {
	return configurationElement.getAttribute(ATT_ID);
}
public void setImage(Image image) {
	this.image = image;
}
}
