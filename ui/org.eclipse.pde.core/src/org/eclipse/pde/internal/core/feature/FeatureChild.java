package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
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

	protected void reset() {
		super.reset();
		version = null;
	}
	protected void parse(Node node) {
		super.parse(node);
		version = getNodeAttribute(node, "version");
		hookWithWorkspace();
	}
	
	public void loadFrom(IFeature feature) {
		id = feature.getId();
		version = feature.getVersion();
		this.feature = feature;
	}
	/**
	 * @see IFeatureChild#getVersion()
	 */
	public String getVersion() {
		return version;
	}

	public IFeature getReferencedFeature() {
		if (feature==null)
			hookWithWorkspace();
		return feature;
	}

	private void hookWithWorkspace() {
		IFeatureModel[] models =
			PDECore.getDefault().getWorkspaceModelManager().getWorkspaceFeatureModels();
		for (int i = 0; i < models.length; i++) {
			IFeature feature = models[i].getFeature();
			
			if (feature!=null && feature.getId().equals(getId())) {
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
	
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_VERSION)) {
			setVersion((String)newValue);
		}
		else super.restoreProperty(name, oldValue, newValue);
	}
	
	public void setId(String id) throws CoreException {
		super.setId(id);
		hookWithWorkspace();
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
		writer.println(">");
		writer.println(indent + "</includes>");
	}
}