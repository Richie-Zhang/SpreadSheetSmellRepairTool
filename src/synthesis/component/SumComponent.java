package synthesis.component;

import synthesis.basic.IOType;
import synthesis.basic.Type;
import synthesis.basic.VarType;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Z3Exception;

public final class SumComponent extends Component {

	int num = 0;

	public SumComponent(int num) {
		this.num = num;
		type = Components.SUM;
		compId++;

		Type inType = new Type(IOType.COMP_INPUT, VarType.INTEGER);
		Type outType = new Type(IOType.COMP_OUTPUT, VarType.INTEGER);
		for (int i = 0; i < num; i++) {
			varTypes.add(inType);
		}
		varTypes.add(outType);
	}

	public void init() throws Z3Exception {
		super.init();

		if (num < 2) {
			spec = ctx.mkEq(variables.get(0), variables.get(1));
		} else {
			ArithExpr sum = ctx.mkAdd((IntExpr) variables.get(0),
					(IntExpr) variables.get(1));
			for (int i = 2; i < num; i++) {
				sum = ctx.mkAdd(sum, (IntExpr) variables.get(i));
			}
			spec = ctx.mkEq(sum, variables.get(num));
		}
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
