package synthesis.component;

import java.util.ArrayList;

import choco.Choco;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;

public final class SumComponent extends Component {
	int num = 0;

	public SumComponent(int num) {
		this.num = num;

		id = Components.SUM;

		inputs = new ArrayList<IntegerVariable>();
		for (int i = 0; i < num; i++) {
			inputs.add(Choco.makeIntVar("sum_para_" + i));
		}

		output = Choco.makeIntVar("sum_result");

		if (num < 2) {
			spec = Choco.eq(inputs.get(0), output);
		} else {
			IntegerExpressionVariable temp = Choco.plus(inputs.get(0),
					inputs.get(1));
			for (int i = 2; i < num; i++) {
				temp = Choco.plus(temp, inputs.get(i));
			}
			spec = Choco.eq(temp, output);
		}

		canSwap = true;
	}

	public int getNum() {
		return num;
	}

	public String getProg(String[] paras) {
		StringBuffer sb = new StringBuffer();
		sb.append("SUM(");
		for (int i = 0; i < paras.length - 1; i++) {
			sb.append(paras[i] + ",");
		}
		sb.append(paras[paras.length - 1]);
		sb.append(")");
		return sb.toString();
	}

	public boolean equals(Object obj) {
		if (obj instanceof SumComponent) {
			if (((SumComponent) obj).getNum() == this.num) {
				return true;
			}
		}
		return false;
	}

}
