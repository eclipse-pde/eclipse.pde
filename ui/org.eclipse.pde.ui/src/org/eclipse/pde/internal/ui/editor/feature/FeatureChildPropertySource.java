package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.views.properties.*;

public class FeatureChildPropertySource extends FeaturePropertySource {
	protected Vector descriptors;
	public final static String KEY_ID = "FeatureEditor.ChildProp.id";
	public final static String KEY_VERSION = "FeatureEditor.ChildProp.version";
	public final static String KEY_OPTIONAL =
		"FeatureEditor.ChildProp.optional";
	public final static String KEY_MATCH = "FeatureEditor.ChildProp.match";
	public final static String KEY_NAME = "FeatureEditor.ChildProp.name";
	public final static String KEY_SEARCH_LOCATION =
		"FeatureEditor.ChildProp.search-location";
	private final static String P_ID = "id";
	private final static String P_VERSION = "version";
	private final static String P_OPTIONAL = "optional";
	private final static String P_MATCH = "match";
	private final static String P_NAME = "name";
	private final static String P_SEARCH_LOCATION = "search-location";

	public FeatureChildPropertySource(IFeatureChild child) {
		super(child);
	}

	protected void createPropertyDescriptors() {
		descriptors = new Vector();
		PropertyDescriptor desc =
			createTextPropertyDescriptor(
				P_ID,
				PDEPlugin.getResourceString(KEY_ID));
		descriptors.addElement(desc);
		desc =
			createTextPropertyDescriptor(
				P_VERSION,
				PDEPlugin.getResourceString(KEY_VERSION));
		descriptors.addElement(desc);

		desc =
			createTextPropertyDescriptor(
				P_NAME,
				PDEPlugin.getResourceString(KEY_NAME));
		descriptors.addElement(desc);

		desc =
			createChoicePropertyDescriptor(
				P_MATCH,
				PDEPlugin.getResourceString(KEY_MATCH),
				IMatchRules.RULE_NAME_TABLE);
		descriptors.addElement(desc);
		desc =
			createChoicePropertyDescriptor(
				P_OPTIONAL,
				PDEPlugin.getResourceString(KEY_OPTIONAL),
				new String[] { "false", "true" });
		descriptors.addElement(desc);
		desc =
			createChoicePropertyDescriptor(
				P_SEARCH_LOCATION,
				PDEPlugin.getResourceString(KEY_SEARCH_LOCATION),
				new String[] { "root", "self", "both" });
		descriptors.addElement(desc);
	}

	public IFeatureChild getChild() {
		return (IFeatureChild) object;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (descriptors == null) {
			createPropertyDescriptors();
		}
		return toDescriptorArray(descriptors);
	}

	public Object getPropertyValue(Object name) {
		if (name.equals(P_ID)) {
			return getNonzeroValue(getChild().getId());
		}

		if (name.equals(P_VERSION)) {
			return getNonzeroValue(getChild().getVersion());
		}

		if (name.equals(P_OPTIONAL)) {
			return getChild().isOptional() ? new Integer(1):new Integer(0);
		}
		if (name.equals(P_NAME)) {
			return getChild().getName();
		}
		if (name.equals(P_SEARCH_LOCATION)) {
			int loc = getChild().getSearchLocation();
			return new Integer(loc);
		}
		if (name.equals(P_MATCH)) {
			return new Integer(getChild().getMatch());
		}
		return null;
	}

	private String getNonzeroValue(Object obj) {
		return obj != null ? obj.toString() : "";
	}
	
	public void setElement(IFeatureEntry entry) {
		object = entry;
	}
	
	public void setPropertyValue(Object name, Object value) {
		String svalue = value.toString();
		String realValue =
			svalue == null | svalue.length() == 0 ? null : svalue;
		try {
			if (name.equals(P_ID)) {
				getChild().setId(realValue);
			} else if (name.equals(P_VERSION)) {
				getChild().setVersion(realValue);
			} else if (name.equals(P_NAME)) {
				getChild().setName(realValue);
			} else if (name.equals(P_OPTIONAL)) {
				Integer index = (Integer) value;
				getChild().setOptional(index.intValue() == 1);
			} else if (name.equals(P_MATCH)) {
				Integer index = (Integer) value;
				getChild().setMatch(index.intValue());
			} else if (name.equals(P_SEARCH_LOCATION)) {
				Integer index = (Integer) value;
				getChild().setSearchLocation(index.intValue());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}