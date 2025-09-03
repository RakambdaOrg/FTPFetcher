package fr.rakambda.ftpfetcher;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import java.util.LinkedList;
import java.util.function.Function;

@Getter
@Setter
public class SumSplitCollection<T> extends LinkedList<T> implements Comparable<SumSplitCollection<?>>{
	private final Function<T, Long> propertyExtractor;
	private long propertySum;
	
	public SumSplitCollection(Function<T, Long> propertyExtractor){
		super();
		this.propertyExtractor = propertyExtractor;
		propertySum = 0L;
	}
	
	@Override
	public boolean add(T t){
		if(super.add(t)){
			propertySum = propertySum + propertyExtractor.apply(t);
			return true;
		}
		return false;
	}
	
	@Override
	public int compareTo(@NonNull SumSplitCollection<?> o){
		return Long.compare(getPropertySum(), o.getPropertySum());
	}
}
