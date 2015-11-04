package info.ryankenney.async_driver;

public interface AsyncTask<A,R> extends Task {

	void run(A arg, ResultHandler<R> resultHandler);
	
}
