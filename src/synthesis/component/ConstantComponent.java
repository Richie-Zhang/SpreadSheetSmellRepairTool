package synthesis.component;

import synthesis.basic.IOType;
import synthesis.basic.Type;
import synthesis.basic.VarType;

import com.microsoft.z3.Z3Exception;

public final class ConstantComponent extends Component {

	public ConstantComponent() {
		type = Components.CONSTANT;
		compId++;

		Type outType = new Type(IOType.COMP_OUTPUT, VarType.INTEGER);
		varTypes.add(outType);
	}

	public void init() throws Z3Exception {
		super.init();

		spec = ctx.mkBool(true);
	}

	public String getProg(String[] paras) {
		return paras[0];
	}

}
