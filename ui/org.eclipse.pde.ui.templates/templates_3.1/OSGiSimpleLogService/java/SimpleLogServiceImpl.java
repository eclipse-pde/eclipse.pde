package $packageName$;

public class SimpleLogServiceImpl implements SimpleLogService {

	@Override
	public void log(String message) {
		System.out.println(message);
	}
	
}
