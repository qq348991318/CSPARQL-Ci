package eu.larkc.csparql.core.imis.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;

public class ECSTuple {

	public int property ;
	
	public ExtendedCharacteristicSet ecs ;
	
	public TripleAsInt triplePattern ;
	
	public HashMap<Integer, Integer> subjectBinds;
	public HashMap<Integer, Integer> objectBinds;
	
	public ECSTuple(ExtendedCharacteristicSet ecs, int property, TripleAsInt triplePattern){
		
		this.property = property;
		
		this.ecs = ecs;
		
		this.triplePattern = triplePattern;
	}
	
	@Override
    public int hashCode() {		
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers            
            append(ecs).        	
            toHashCode();
    }
	
	@Override
    public boolean equals(Object obj) {
       if (!(obj instanceof ECSTuple))
            return false;
        if (obj == this)
            return true;

        ECSTuple rhs = (ECSTuple) obj;
        return new EqualsBuilder().            
        	append(ecs, rhs.ecs).
            isEquals();
    }
	
	@Override
	public String toString(){
		return ecs.toString();
	}
	
}
