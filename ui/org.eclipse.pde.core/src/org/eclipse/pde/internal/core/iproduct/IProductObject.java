package org.eclipse.pde.internal.core.iproduct;

import java.io.*;

import org.eclipse.pde.core.*;
import org.w3c.dom.*;


public interface IProductObject extends IWritable, Serializable{
	
	IProductModel getModel();
	
	IProduct getProduct();
	
	boolean isInTheModel();
	
	void setInTheModel(boolean inTheModel);
	
	boolean isValid();
	
	void parse(Node node);
	
}
