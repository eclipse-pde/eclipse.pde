package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class FeatureChild extends IdentifiableObject implements IFeatureChild {
	private String version;
	private IFeature feature;
	private String name;
	private boolean optional;
	private int searchLocation = LOCATION_ROOT;
	private int match = IMatchRules.PERFECT;

	protected void reset() {
		super.reset();
		version = null;
		optional = false;
		name = null;
		searchLocation = LOCATION_ROOT;
		match = IMatchRules.PERFECT;
	}
	protected void parse(Node node) {
		super.parse(node);
		version = getNodeAttribute(node, "version");
		name = getNodeAttribute(node, "name");
		optional = getBooleanAttribute(node, "optional");
		String value = getNodeAttribute(node, "search-location");
		if (value != null) {
			if (value.equalsIgnoreCase("root")) {
				searchLocation = LOCATION_ROOT;
			} else if (value.equalsIgnoreCase("self")) {
				searchLocation = LOCATION_SELF;
			} else if (value.equalsIgnoreCase("both")) {
				searchLocation = LOCATION_BOTH;
			}
		}
		value = getNodeAttribute(node, "match");
		if (value != null) {
			for (int i = 0; i < IMatchRules.RULE_NAME_TABLE.length; i++) {
				if (value.equals(IMatchRules.RULE_NAME_TABLE[i])) {
					match = i;
					break;
				}
			}
		}
		hookWithWorkspace();
	}

	public void loadFrom(IFeature feature) {
		id = feature.getId();
		version = feature.getVersion();
		optional = false;
		name = feature.getLabel();
		match = IMatchRules.PERFECT;
		searchLocation = LOCATION_ROOT;
		this.feature = feature;
	}
	/**
	 * @see IFeatureChild#getVersion()
	 */
	public String getVersion() {
		return version;
	}

	public boolean isOptional() {
		return optional;
	}

	public String getName() {
		return name;
	}

	public int getMatch() {
		return match;
	}

	public int getSearchLocation() {
		return searchLocation;
	}

	public IFeature getReferencedFeature() {
		if (feature == null)
			hookWithWorkspace();
		return feature;
	}

	private void hookWithWorkspace() {
		IFeatureModel[] models =
			PDECore
				.getDefault()
				.getWorkspaceModelManager()
				.getWorkspaceFeatureModels();
		for (int i = 0; i < models.length; i++) {
			IFeature feature = models[i].getFeature();

			if (feature != null && feature.getId().equals(getId())) {
				if (version == null || feature.getVersion().equals(version)) {
					this.feature = feature;
					break;
				}
			}
		}
	}

	/**
	 * @see IFeatureChild#setVersion(String)
	 */
	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.version;
		this.version = version;
		firePropertyChanged(P_VERSION, oldValue, version);
		hookWithWorkspace();
	}

	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	public void setOptional(boolean optional) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(this.optional);
		this.optional = optional;
		firePropertyChanged(P_NAME, oldValue, new Boolean(optional));
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion((String) newValue);
		} else if (name.equals(P_OPTIONAL)) {
			setOptional(((Boolean) newValue).booleanValue());
		} else if (name.equals(P_NAME)) {
			setName((String) newValue);
		} else if (name.equals(P_SEARCH_LOCATION)) {
			setSearchLocation(((Integer) newValue).intValue());
		} else if (name.equals(P_MATCH)) {
			setMatch(((Integer) newValue).intValue());
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	public void setId(String id) throws CoreException {
		super.setId(id);
		hookWithWorkspace();
	}

	public void setMatch(int match) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Integer(this.match);
		this.match = match;
		firePropertyChanged(P_MATCH, oldValue, new Integer(match));
	}

	public void setSearchLocation(int searchLocation) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Integer(this.searchLocation);
		this.match = match;
		firePropertyChanged(
			P_SEARCH_LOCATION,
			oldValue,
			new Integer(searchLocation));
	}

	/**
	 * @see IWritable#write(String, PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<includes");
		String indent2 = indent + Feature.INDENT + Feature.INDENT;
		if (getId() != null) {
			writer.println();
			writer.print(indent2 + "id=\"" + getId() + "\"");
		}
		if (getVersion() != null) {
			writer.println();
			writer.print(indent2 + "version=\"" + getVersion() + "\"");
		}
		if (getName() != null) {
			writer.println();
			writer.print(indent2 + "name=\"" + getName() + "\"");
		}
		if (isOptional()) {
			writer.println();
			writer.print(indent2 + "optional=\"true\"");
		}
		if (getMatch() != IMatchRules.PERFECT) {
			writer.println();
			writer.print(
				indent2
					+ "match=\""
					+ IMatchRules.RULE_NAME_TABLE[match]
					+ "\"");
		}
		if (getSearchLocation() != LOCATION_ROOT) {
			writer.println();
			String value = "self";
			if (getSearchLocation() == LOCATION_BOTH)
				value = "both";
			writer.print(indent2 + "search-location=\"" + value + "\"");
		}
		writer.println(">");
		writer.println(indent + "</includes>");
	}
}