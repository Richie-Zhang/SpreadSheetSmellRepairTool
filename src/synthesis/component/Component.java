package synthesis.component;

import java.util.List;

import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;

public abstract class Component {
	
	int id;
	
	List<IntegerVariable> inputs;

	IntegerVariable output;

	Constraint spec;
	
	String prog;
	
	boolean canSwap = false;

	public int getId() {
		return id;
	}
	
	public List<IntegerVariable> getInputs() {
		return inputs;
	}

	public IntegerVariable getOutput() {
		return output;
	}

	public Constraint getSpecification() {
		return spec;
	}
	
	public boolean canSwap() {
		return canSwap;
	}
	
	public abstract String getProg(String[] paras); 
}
