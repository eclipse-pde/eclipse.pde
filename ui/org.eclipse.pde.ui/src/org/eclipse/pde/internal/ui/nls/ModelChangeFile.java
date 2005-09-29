package org.eclipse.pde.internal.ui.nls;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;

public class ModelChangeFile {
	private IFile fFile;
	private ModelChange fModel;
	private ArrayList fChanges = new ArrayList();
	private int fNumChanges = 0;
	protected ModelChangeFile (IFile file, ModelChange model) {
		fFile = file;
		fModel = model;
	}
	protected IFile getFile() {
		return fFile;
	}
	protected ModelChange getModel() {
		return fModel;
	}
	protected void add(ModelChangeElement element) {
		if (fChanges.add(element))
			fNumChanges += 1;
	}
	protected int getNumChanges() {
		return fNumChanges;
	}
	protected ArrayList getChanges() {
		return fChanges;
	}
}
