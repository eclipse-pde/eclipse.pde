package org.example.b;

import java.util.ArrayList;

import org.example.a.Sub;

public class B {

	public void method() {
		ArrayList list = new ArrayList();
		list.add(this);
		
		Sub d = new Sub();
		d.doSomething(list);
	}
}
