package org.eclipse.pde.internal.builders;

import org.eclipse.core.resources.*;
import org.w3c.dom.*;


public class FragmentErrorReporter extends PluginBaseErrorReporter {

	public FragmentErrorReporter(IFile file) {
		super(file);
	}
	
	 /* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.PluginBaseErrorReporter#validateTopLevelAttributes(org.w3c.dom.Element)
	 */
	protected void validateTopLevelAttributes(Element element) {
		super.validateTopLevelAttributes(element);
		
		if (assertAttributeDefined(element, "plugin-id", CompilerFlags.ERROR))
			validatePluginID(element, element.getAttributeNode("plugin-id"));
		
		if (assertAttributeDefined(element, "plugin-version", CompilerFlags.ERROR))
			validateVersionAttribute(element, element.getAttributeNode("plugin-version"));
		
		Attr attr = element.getAttributeNode("match");
		if (attr != null)
			validateMatch(element, attr);
	}
	
	protected String getRootElementName() {
		return "fragment";
	}
	
}
