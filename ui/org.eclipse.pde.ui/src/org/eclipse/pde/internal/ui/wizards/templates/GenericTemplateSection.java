package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.ui.IPluginStructureData;

public abstract class GenericTemplateSection extends AbstractTemplateSection {
	protected Hashtable options = new Hashtable();

	protected TemplateOption addOption(
		String name,
		String label,
		boolean value,
		ArrayList list) {
		BooleanOption option = new BooleanOption(this, name, label);
		addOption(option, value ? Boolean.TRUE : Boolean.FALSE, list);
		return option;
	}

	protected TemplateOption addOption(
		String name,
		String label,
		String value,
		ArrayList list) {
		StringOption option = new StringOption(this, name, label);
		addOption(option, value, list);
		return option;
	}

	protected TemplateOption addOption(
		String name,
		String label,
		String[][] choices,
		String value,
		ArrayList list) {
		ChoiceOption option = new ChoiceOption(this, name, label, choices);
		addOption(option, value, list);
		return option;
	}

	private void addOption(TemplateOption option, Object value, ArrayList list) {
		option.setValue(value);
		list.add(option);
		registerOption(option);
	}

	protected void initializeOption(String key, Object value) {
		TemplateOption option = getOption(key);
		if (option != null) {
			// Only initialize options that have no value set
			if (option.getValue() == null)
				option.setValue(value);
		}
	}

	protected void registerOption(TemplateOption option) {
		options.put(option.getName(), option);
	}

	public String getStringOption(String key) {
		TemplateOption option = (TemplateOption) options.get(key);
		if (option != null && option instanceof StringOption) {
			return ((StringOption) option).getText();
		}
		return null;
	}

	public boolean getBooleanOption(String key) {
		TemplateOption option = (TemplateOption) options.get(key);
		if (option != null && option instanceof BooleanOption) {
			return ((BooleanOption) option).isSelected();
		}
		return false;
	}

	public void setOptionEnabled(String key, boolean enabled) {
		TemplateOption option = (TemplateOption) options.get(key);
		if (option != null)
			option.setEnabled(enabled);
	}

	public Object getValue(String key) {
		TemplateOption option = (TemplateOption) options.get(key);
		if (option != null)
			return option.getValue();
		return super.getValue(key);
	}

	protected TemplateOption getOption(String key) {
		return (TemplateOption) options.get(key);
	}

	public boolean isDependentOnFirstPage() {
		return false;
	}

	protected void initializeFields(IPluginStructureData sdata, FieldData data) {
	}

	public void initializeFields(IPluginModelBase model) {
	}

	public abstract void validateOptions(TemplateOption changed);

	public void execute(
		IProject project,
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {
		initializeFields(model);
		super.execute(project, model, monitor);
	}
}