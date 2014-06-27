package synthesis.basic;

import java.util.ArrayList;
import java.util.List;

import synthesis.basic.Program.Line;
import synthesis.basic.Program.LineType;
import synthesis.component.Component;
import synthesis.component.Components;

public class Lval2Prog {
	public static String[] tranform(Result res, ProgramAbstract pa, boolean isFormula) {
		Program prog = new Program(res, pa);
		Line[] lines = prog.tranform();

		/*if (isFormula) {
			Line resLine = lines[lines.length - 1];
			Integer ret = resLine.inputs[0];
			String inputs[] = new String[pa.inputTypes.size()];
			for (int i = 0; i < pa.inputTypes.size(); i++) {
				inputs[i] = "x" + (i + 1);
			}
			String formula = constructPara(ret, lines, inputs);
			
			StringBuffer result = new StringBuffer();
			result.append("f(");
			for (int i=0; i<inputs.length; i++) {
				result.append(inputs[i] + ",");
			}
			result.delete(result.length()-1, result.length());
			result.append(") = ");
			result.append(formula);
			System.out.println(result);
		} else {*/
			String progs[] = new String[lines.length];
			for (int i = 0; i < lines.length; i++) {
				Line line = lines[i];
				LineType type = line.type;
				if (type == LineType.INPUT) {
					progs[i] = "o" + i + " = " + "i" + line.inputs[0];
				} else if (type == LineType.OUTPUT) {
					progs[i] = "ret " + "o" + line.inputs[0];
				} else {
					String right = getComponentProg(line.comp, line.inputs);
					progs[i] = "o" + i + " = " + right;
				}
			}

			for (String str : progs) {
				System.out.println(str);
			}
			return progs;
		//}
	}

	private static String getComponentProg(Component comp, Integer[] inputs) {
		List<String> paras = new ArrayList<String>();

		if (comp.getType() == Components.CONSTANT) {
			paras.add("" + inputs[0]);
		} else {
			for (Integer input : inputs) {
				String para = "o" + input;
				paras.add(para);
			}
		}

		return comp.getProg(paras.toArray(new String[0]));
	}

	@SuppressWarnings("unused")
	private static String constructPara(int para, Line[] lines, String[] inputs) {

		Line paraLine = lines[para];

		if (paraLine.type == LineType.INPUT) {
			int index = paraLine.inputs[0];
			return inputs[index];
		} else if (paraLine.type == LineType.COMPONENT) {
			if (paraLine.comp.getType() == Components.CONSTANT) {
				return "" + paraLine.inputs[0];
			}
			String[] paras = new String[paraLine.inputs.length];
			for (int i = 0; i < paraLine.inputs.length; i++) {
				paras[i] = constructPara(paraLine.inputs[i], lines, inputs);
			}
			return "(" + paraLine.comp.getProg(paras) + ")";
		}

		return null;
	}
}
