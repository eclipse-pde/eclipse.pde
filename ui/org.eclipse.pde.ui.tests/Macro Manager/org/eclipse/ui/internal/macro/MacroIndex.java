package org.eclipse.ui.internal.macro;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Node;

public class MacroIndex implements IWritable, IPlayable {
	private String id;
	
	public MacroIndex() {
	}
	
	public MacroIndex(String id) {
		this.id = id;
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<index id=\"");
		writer.print(id);
		writer.println("\"/>");
	}

	void load(Node node) {
		this.id = MacroUtil.getAttribute(node, "id");
	}
	
	public String getId() {
		return id;
	}

	public boolean playback(Display display, Composite parent, IProgressMonitor monitor) throws CoreException {
		return false;
	}
}