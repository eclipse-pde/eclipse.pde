package org.eclipse.pde.internal.wizards.extension;

import org.eclipse.ui.part.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.*;
import org.eclipse.core.runtime.OperationCanceledException;
import java.lang.reflect.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.schema.*;
import java.io.*;
import org.eclipse.pde.internal.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.jface.window.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.editor.schema.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.codegen.*;

public class NewSchemaFileMainPage extends BaseExtensionPointMainPage {
	public static final String KEY_TITLE = "NewSchemaFileWizard.title";
	public static final String KEY_DESC = "NewSchemaFileWizard.desc";

public NewSchemaFileMainPage(IContainer container) {
	super(container);
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
}
public boolean finish() {
	IRunnableWithProgress operation = getOperation();
	try {
		getContainer().run(false, true, operation);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	} catch (InterruptedException e) {
		return false;
	}
	return true;
}
protected boolean isPluginIdNeeded() {
	return true;
}
}
