package synthesis.component;

import synthesis.basic.IOType;
import synthesis.basic.Type;
import synthesis.basic.VarType;

import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Z3Exception;

public final class AbsComponent extends Component {

	public AbsComponent() {
		type = Components.ABS;
		compId++;

		Type inType = new Type(IOType.COMP_INPUT, VarType.INTEGER);
		Type outType = new Type(IOType.COMP_OUTPUT, VarType.INTEGER);
		varTypes.add(inType);
		varTypes.add(outType);
	}

	public void init() throws Z3Exception {
		super.init();

		spec = ctx.mkEq(
				ctx.mkITE(ctx.mkLt((IntExpr) variables.get(0), ctx.mkInt(0)),
						ctx.mkUnaryMinus((IntExpr) variables.get(0)),
						variables.get(0)), variables.get(1));
	}

	public String getProg(String[] paras) {
		return "abs(" + paras[0] + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AbsComponent) {
			return true;
		}
		return false;
	}
}
