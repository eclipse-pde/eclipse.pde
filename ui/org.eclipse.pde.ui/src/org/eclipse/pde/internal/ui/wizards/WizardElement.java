package org.eclipse.pde.internal.ui.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.ui.templates.ITemplateSection;

import java.util.*;


public class WizardElement extends NamedElement {
	public static final String ATT_NAME = "name";
	public static final String TAG_DESCRIPTION = "description";
	public static final String ATT_ICON = "icon";
	public static final String ATT_ID = "id";
	public static final String ATT_CLASS = "class";
	public static final String ATT_TEMPLATE = "template";
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
			description = expandDescription(children[0].getValue());
		}
	}
	return description;
}

/**
 * We allow replacement variables in description values as well.
 * This is to allow extension template descriptin reuse in
 * project template wizards. Tokens in form '%token%' will
 * be evaluated against the contributing plug-in's resource
 * bundle. As before, to have '%' in the description, one
 * need to add '%%'.
 */

private String expandDescription(String source) {
	if (source==null || source.length()==0) return source;
	if (source.indexOf('%')== -1) return source;
	ResourceBundle bundle = configurationElement.getDeclaringExtension().getDeclaringPluginDescriptor().getResourceBundle();
	if (bundle==null) return source;
	StringBuffer buf = new StringBuffer();
	boolean keyMode = false;
	int keyStartIndex = -1;
	for (int i=0; i<source.length(); i++) {
		char c = source.charAt(i);
		if (c=='%') {
			char c2 = source.charAt(i+1);
			if (c2 == '%') {
				i++;
				buf.append('%');
				continue;
			}
			else {
				if (keyMode) {
					keyMode = false;
					String key = source.substring(keyStartIndex, i);
					String value = key;
					try {
						value = bundle.getString(key);
					}
					catch (MissingResourceException e) {
					}
					buf.append(value);
				}
				else {
					keyStartIndex = i+1;
					keyMode = true;
				}
			}
		}
		else if (!keyMode) {
			buf.append(c);
		}
	}
	return buf.toString();
}

public String getID() {
	return configurationElement.getAttribute(ATT_ID);
}
public void setImage(Image image) {
	this.image = image;
}

public boolean isTemplate() {
	String att = configurationElement.getAttribute(ATT_TEMPLATE);
	if (att!=null && att.equalsIgnoreCase("true")) return true;
	return false;
}
}
