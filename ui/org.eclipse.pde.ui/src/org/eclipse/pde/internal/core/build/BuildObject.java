package org.eclipse.pde.internal.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.build.*;
import java.util.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;

public class BuildObject {
	private IBuildModel model;

public BuildObject() {
}
protected void ensureModelEditable() throws CoreException {
	if (!model.isEditable()) {
		throwCoreException("Illegal attempt to change read-only build.properties");
	}
}
public IBuildModel getModel() {
	return model;
}
void setModel(IBuildModel newModel) {
	model = newModel;
}
protected void throwCoreException(String message) throws CoreException {
	Status status =
		new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
	throw new CoreException(status);
}
}
