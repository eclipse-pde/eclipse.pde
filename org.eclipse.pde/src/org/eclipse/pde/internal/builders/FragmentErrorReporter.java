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
		
		if (assertAttributeDefined(element, "plugin-id", CompilerFlags.ERROR)) //$NON-NLS-1$
			validatePluginID(element, element.getAttributeNode("plugin-id")); //$NON-NLS-1$
		
		if (assertAttributeDefined(element, "plugin-version", CompilerFlags.ERROR)) //$NON-NLS-1$
			validateVersionAttribute(element, element.getAttributeNode("plugin-version")); //$NON-NLS-1$
		
		Attr attr = element.getAttributeNode("match"); //$NON-NLS-1$
		if (attr != null)
			validateMatch(element, attr);
	}
	
	protected String getRootElementName() {
		return "fragment"; //$NON-NLS-1$
	}
	
}
