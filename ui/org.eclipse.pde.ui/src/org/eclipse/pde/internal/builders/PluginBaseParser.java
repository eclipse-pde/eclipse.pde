/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.builders;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.xml.sax.Attributes;

public abstract class PluginBaseParser extends AbstractParser {
	
	// Valid States
	private final int PLUGIN_RUNTIME_STATE = 101;
	private final int PLUGIN_REQUIRES_STATE = 102;
	private final int PLUGIN_EXTENSION_POINT_STATE = 103;
	private final int PLUGIN_EXTENSION_STATE = 104;
	private final int RUNTIME_LIBRARY_STATE = 105;
	private final int LIBRARY_EXPORT_STATE = 106;
	private final int PLUGIN_REQUIRES_IMPORT_STATE = 107;
	private final int DESCRIPTION_STATE = 108;
	
	public PluginBaseParser(IPluginModelBase model) {
		super(model);
	}

	/*
	 * @see AbstractParser#canAcceptText(int)
	 */
	protected boolean canAcceptText(int state) {
		return false;
	}

	/*
	 * @see AbstractParser#acceptText(String)
	 */
	protected void acceptText(String text) {
	}

	/*
	 * @see AbstractParser#handleErrorStatus(IStatus)
	 */
	protected void handleErrorStatus(IStatus status) {
	}
	
	protected void handleState(int state, String elementName, Attributes attributes) {
		switch (state) {
		case PLUGIN_RUNTIME_STATE :
			handleRuntimeState(elementName, attributes);
			break;
		case PLUGIN_REQUIRES_STATE :
			handleRequiresState(elementName, attributes);
			break;
		case PLUGIN_EXTENSION_POINT_STATE :
			handleExtensionPointState(elementName, attributes);
			break;
		case PLUGIN_EXTENSION_STATE :
			handleExtensionState(elementName, attributes);
			break;
		case RUNTIME_LIBRARY_STATE :
			handleLibraryState(elementName, attributes);
			break;
		case LIBRARY_EXPORT_STATE :
			handleLibraryExportState(elementName, attributes);
			break;
		case PLUGIN_REQUIRES_IMPORT_STATE :
			handleRequiresImportState(elementName, attributes);
			break;
		case DESCRIPTION_STATE :
			handleDescriptionState(elementName, attributes);
			break;
		default:
			super.handleState(state, elementName, attributes);
			break;
		}
	}

	public void handleExtensionPointState(String elementName, Attributes attributes) {
		// We ignore all elements under extension points (if there are any)
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		internalError(Policy.bind("parse.unknownElement", EXTENSION_POINT, elementName));
	}

	/*
	 * @see AbstractParser#handleEndState(int, String)
	 */
	protected void handleEndState(int state, String elementName) {
	}
}
