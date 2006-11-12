package $packageName$;

public class HelloServiceImpl implements HelloService {
	
	public void speak() {
		System.out.println("$greeting$");
	}
	
	public void yell() {
		System.out.println("$greeting$".toUpperCase().concat("!!!")); //$NON-NLS-1$
	}

}
