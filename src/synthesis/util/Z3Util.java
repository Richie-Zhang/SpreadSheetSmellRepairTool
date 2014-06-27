package synthesis.util;

import java.util.HashMap;

import synthesis.basic.Type;
import synthesis.basic.VarType;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;

public class Z3Util {

	public static int id = 0;

	public static String getVarName() {
		return "temp_" + id++;
	}

	public static Context getContext() {
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		try {
			return new Context(cfg);
		} catch (Z3Exception e) {
			return null;
		}
	}

	public static Expr getVariable(Type type, Context ctx, String varName)
			throws Z3Exception {
		if (type.varType == VarType.INTEGER) {
			return ctx.mkIntConst(varName);
		} else if (type.varType == VarType.BOOLEAN) {
			return ctx.mkBoolConst(varName);
		} else if (type.varType == VarType.ARRAY) {
			return ctx
					.mkArrayConst(varName, ctx.getIntSort(), ctx.getIntSort());
		}
		return null;
	}
}
