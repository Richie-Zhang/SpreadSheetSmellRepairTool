package synthesis.component;

import java.util.ArrayList;
import java.util.List;

public class Components {
	public static int index = 1;
	public final static int PLUS = index++;
	public final static int MINUS = index++;
	public final static int MULT = index++;
	public final static int CONSTANT = index++;
	public final static int SUM = index++;
	public final static int IFELSE = index++;
	public final static int EQUAL = index++;
	public final static int NOT = index++;
	public final static int GT = index++;
	public final static int GE = index++;
	public final static int LT = index++;
	public final static int LE = index++;
	public final static int AND = index++;
	public final static int OR = index++;
	public final static int ABS = index++;

	public static List<Component> getComponents(List<Component> components) {
		List<Component> newComps = new ArrayList<Component>();
		for (Component comp : components) {
			Component c = null;
			if (comp.type == PLUS) {
				c = new PlusComponent();
			} else if (comp.type == MINUS) {
				c = new MinusComponent();
			} else if (comp.type == MULT) {
				c = new MultComponent();
			} else if (comp.type == CONSTANT) {
				c = new ConstantComponent();
			} else if (comp.type == SUM) {
				c = new SumComponent(((SumComponent) comp).getNum());
			} else if (comp.type == ABS) {
				c = new AbsComponent();
			} 
			try {
				c.init();
			} catch (Exception e) {
				System.err.print(e);
			}
			newComps.add(c);
		}
		return newComps;
	}
}
