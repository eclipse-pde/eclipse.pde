package org.eclipse.pde.internal.core;

import java.io.File;

public class EntryFileAdapter extends FileAdapter {
	private ModelEntry entry;

	/**
	 * Constructor for EntryFileAdapter.
	 * @param parent
	 * @param file
	 */
	public EntryFileAdapter(ModelEntry entry, File file, IFileAdapterFactory factory) {
		super(null, file, factory);
		this.entry = entry;
	}
	
	public ModelEntry getEntry() {
		return entry;
	}
}