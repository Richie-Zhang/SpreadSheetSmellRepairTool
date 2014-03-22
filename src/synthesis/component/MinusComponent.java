package synthesis.component;

import java.util.ArrayList;

import choco.Choco;
import choco.kernel.model.variables.integer.IntegerVariable;

public final class MinusComponent extends Component {
	public MinusComponent() {
		id = Components.MINUS;
		
		inputs = new ArrayList<IntegerVariable>();
		for (int i = 0; i < 2; i++) {
			inputs.add(Choco.makeIntVar("minus_para_" + i));
		}

		output = Choco.makeIntVar("minus_result");

		spec = Choco.eq(Choco.minus(inputs.get(0), inputs.get(1)), output);
	}

	public String getProg(String[] paras) {
		return paras[0] + "-" + paras[1];
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MinusComponent){
			return true;
		} 
		return false;
	}
}
