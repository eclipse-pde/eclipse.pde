package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.*;
import java.util.*;

public abstract class GenericTemplateSection extends AbstractTemplateSection {
	protected Hashtable options = new Hashtable();
	
	protected void addOption(String name, String label, boolean value, ArrayList list) {
		BooleanOption option = new BooleanOption(this, name, label);
		option.setSelected(value);
		list.add(option);
		registerOption(option);
	}

	protected void addOption(String name, String label, String value, ArrayList list) {
		StringOption option = new StringOption(this, name, label);
		option.setText(value);
		list.add(option);
		registerOption(option);
	}	
	
	protected void addOption(String name, String label, String [][] choices, String value, ArrayList list) {
		ChoiceOption option = new ChoiceOption(this, name, label, choices);
		option.setValue(value);
		list.add(option);
		registerOption(option);
	}
	
	protected void registerOption(TemplateOption option) {
		options.put(option.getName(), option);
	}
	
	public String getStringOption(String key) {
		TemplateOption option = (TemplateOption)options.get(key);
		if (option!=null && option instanceof StringOption) {
			return ((StringOption)option).getText();
		}
		return null;
	}
	
	public boolean getBooleanOption(String key) {
		TemplateOption option = (TemplateOption)options.get(key);
		if (option!=null && option instanceof BooleanOption) {
			return ((BooleanOption)option).isSelected();
		}
		return false;
	}
	
	public Object getValue(String key) {
		TemplateOption option = (TemplateOption)options.get(key);
		if (option!=null) return option.getValue();
		return super.getValue(key);
	}
	
	public abstract void validateOptions(TemplateOption changed);
}