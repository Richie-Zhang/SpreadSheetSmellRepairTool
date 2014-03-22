package synthesis.component;

import java.util.ArrayList;

import choco.Choco;
import choco.kernel.model.variables.integer.IntegerVariable;

public final class PlusComponent extends Component {
	public PlusComponent() {
		id = Components.PLUS;
		
		inputs = new ArrayList<IntegerVariable>();
		for (int i = 0; i < 2; i++) {
			inputs.add(Choco.makeIntVar("plus_para_" + i));
		}

		output = Choco.makeIntVar("plus_result");

		spec = Choco.eq(Choco.plus(inputs.get(0), inputs.get(1)), output);
		
		canSwap = true;
	}

	public String getProg(String[] paras) {
		return paras[0] + "+" + paras[1];
	}

	public boolean equals(Object obj) {
		if (obj instanceof PlusComponent){
			return true;
		} 
		return false;
	}
}
