package org.eclipse.pde.internal.ui.view;

import org.eclipse.pde.internal.core.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.resources.IResource;

public class ModelEntryPropertySource implements IPropertySource {
	private IPropertyDescriptor [] descriptors;
	private ModelEntry entry;

	/**
	 * Constructor for FileAdapterPropertySource.
	 */
	public ModelEntryPropertySource() {
		super();
	}
	
	public void setEntry(ModelEntry entry) {
		this.entry = entry;
	}

	/**
	 * @see IPropertySource#getEditableValue()
	 */
	public Object getEditableValue() {
		return null;
	}

	/**
	 * @see IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (descriptors==null) {
			descriptors = new IPropertyDescriptor[8];
			descriptors[0] = new PropertyDescriptor("kind", "kind");
			descriptors[1] = new PropertyDescriptor("name", "name");
			descriptors[2] = new PropertyDescriptor("fragment", "fragment");
			descriptors[3] = new PropertyDescriptor("path", "path");
			descriptors[4] = new PropertyDescriptor("id", "id");
			descriptors[5] = new PropertyDescriptor("version", "version");
			descriptors[6] = new PropertyDescriptor("provider", "provider");
			descriptors[7] = new PropertyDescriptor("enabled", "enabled");
		}
		return descriptors;
	}

	/**
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {
		String key = id.toString();
		IPluginModelBase model = entry.getActiveModel();
		IResource resource = model.getUnderlyingResource();
		if (key.equals("enabled"))
			return model.isEnabled()?"true":"false";
		if (key.equals("kind"))
			return resource!=null?"workspace":"external";
		if (key.equals("fragment"))
			return model.isFragmentModel()?"yes":"no";
		if (key.equals("name"))
			return model.getPluginBase().getTranslatedName();
		if (key.equals("path")) {
			if (resource!=null)
				return resource.getLocation().toOSString();
			else
				return model.getInstallLocation();
		}
		if (key.equals("id"))
			return model.getPluginBase().getId();
		if (key.equals("version"))
			return model.getPluginBase().getVersion();
		if (key.equals("provider"))
			return model.getPluginBase().getProviderName();
		return null;
	}

	/**
	 * @see IPropertySource#isPropertySet(Object)
	 */
	public boolean isPropertySet(Object id) {
		return false;
	}

	/**
	 * @see IPropertySource#resetPropertyValue(Object)
	 */
	public void resetPropertyValue(Object id) {
	}

	/**
	 * @see IPropertySource#setPropertyValue(Object, Object)
	 */
	public void setPropertyValue(Object id, Object value) {
	}

}
