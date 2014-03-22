package synthesis.component;

import java.util.ArrayList;

import choco.Choco;
import choco.kernel.model.variables.integer.IntegerVariable;

public final class MultComponent extends Component {
	public MultComponent() {
		id = Components.MULT;
		
		inputs = new ArrayList<IntegerVariable>();
		for (int i = 0; i < 2; i++) {
			inputs.add(Choco.makeIntVar("mult_para_" + i));
		}

		output = Choco.makeIntVar("mult_result");

		spec = Choco.eq(Choco.mult(inputs.get(0), inputs.get(1)), output);
		
		canSwap = true;
	}

	public String getProg(String[] paras) {
		return "(" + paras[0] + ")*(" + paras[1] + ")";
	}

	public boolean equals(Object obj) {
		if (obj instanceof MultComponent){
			return true;
		} 
		return false;
	}
	
}
