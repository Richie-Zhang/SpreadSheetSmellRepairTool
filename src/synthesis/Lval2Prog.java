package synthesis;

import java.util.ArrayList;
import java.util.List;

import synthesis.Program.Line;
import synthesis.Program.LineType;
import synthesis.component.Component;

public class Lval2Prog {
	public static String[] tranform(Result res, List<Type> types,
			List<Component> comps, int numOfInput) {
		Program prog = new Program(res, types, comps, numOfInput);
		Line[] lines = prog.tranform();

		String progs[] = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			Line line = lines[i];
			LineType type = line.type;
			if (type == LineType.INPUT) {
				progs[i] = "o" + i + " = " + "i" + line.inputs[0];
			} else if (type == LineType.OUTPUT) {
				progs[i] = "ret " + "o" + line.inputs[0];
			} else if (type == LineType.CONSTANT) {
				progs[i] = "o" + i + " = " + line.inputs[0];
			} else {
				String right = getComponentProg(line.comp,
						line.inputs);
				progs[i] = "o" + i + " = " + right;
			}
		}
		
		for (String str : progs) {
			System.out.println(str);
		}
		
		return progs;
	}
	
	private static String getComponentProg(Component comp, Integer[] inputs) {
		List<String> paras = new ArrayList<String>();
		for (Integer input : inputs) {
			String para = "o" + input;
			paras.add(para);
		}

		return comp.getProg(paras.toArray(new String[0]));
	}
}
