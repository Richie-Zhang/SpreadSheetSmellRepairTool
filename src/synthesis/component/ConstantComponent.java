package synthesis.component;

import java.util.ArrayList;

import choco.Choco;
import choco.kernel.model.variables.integer.IntegerVariable;

public final class ConstantComponent extends Component {
	public ConstantComponent() {
		id = Components.CONSTANT;
		
		inputs = new ArrayList<IntegerVariable>();

		output = Choco.makeIntVar("constant_result");

		spec = Choco.TRUE;
	}

	public String getProg(String[] paras) {
		return paras[0];
	}

}
