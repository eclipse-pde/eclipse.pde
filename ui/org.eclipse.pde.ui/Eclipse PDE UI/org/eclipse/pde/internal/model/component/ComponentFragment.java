package org.eclipse.pde.internal.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.w3c.dom.Node;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.component.*;

public class ComponentFragment extends ComponentReference implements IComponentFragment {

public void write(String indent, PrintWriter writer) {
	writer.print(indent + "<fragment");
	String indent2 = indent + Component.INDENT + Component.INDENT;
	if (getId() != null) {
		writer.println();
		writer.print(indent2 + "id=\"" + getId() + "\"");
	}
	if (getLabel() != null) {
		writer.println();
		writer.print(indent2 + "label=\"" + getLabel() + "\"");
	}
	if (getVersion() != null) {
		writer.println();
		writer.print(indent2 + "version=\"" + getVersion() + "\"");
	}
	writer.println(">");
	writer.println(indent + "</fragment>");
}
}
