package org.eclipse.pde.ui.templates;

/**
 * @author melhem
 *
 */
public interface IFieldData {
	
	String getId();
	
	String getVersion();
	
	String getName();
	
	String getProvider();
	
	String getLibraryName();
	
	String getSourceFolderName();
	
	String getOutputFolderName();
	
	boolean isLegacy();
	
	boolean hasBundleStructure();
	
	boolean isSimple();
}
