package synthesis.component;

import synthesis.basic.IOType;
import synthesis.basic.Type;
import synthesis.basic.VarType;

import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Z3Exception;

public final class MinusComponent extends Component {

	public MinusComponent() {
		type = Components.MINUS;
		compId++;

		Type inType = new Type(IOType.COMP_INPUT, VarType.INTEGER);
		Type outType = new Type(IOType.COMP_OUTPUT, VarType.INTEGER);
		varTypes.add(inType);
		varTypes.add(inType);
		varTypes.add(outType);
	}

	public void init() throws Z3Exception {
		super.init();

		spec = ctx.mkEq(
				ctx.mkAdd((IntExpr) variables.get(0),
						ctx.mkUnaryMinus((IntExpr) variables.get(1))),
				variables.get(2));
	}

	public String getProg(String[] paras) {
		return paras[0] + " - " + paras[1];
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MinusComponent) {
			return true;
		}
		return false;
	}
}
