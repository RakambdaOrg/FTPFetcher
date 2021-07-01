package fr.raksrinana.ftpfetcher;

import java.util.Deque;
import java.util.LinkedList;

public final class ClosingStack implements AutoCloseable{
	private final Deque<AutoCloseable> resources = new LinkedList<>();
	
	public static class ClosingException extends Exception{}
	
	public <T extends AutoCloseable> T add(T resource){
		resources.addLast(resource);
		return resource;
	}
	
	public void close() throws ClosingException{
		var allClosingExceptions = new ClosingException();
		while(!resources.isEmpty()){
			try{
				resources.removeLast().close();
			}
			catch(Throwable e){
				allClosingExceptions.addSuppressed(e);
			}
		}
		if(allClosingExceptions.getSuppressed().length != 0){
			throw allClosingExceptions;
		}
	}
}
