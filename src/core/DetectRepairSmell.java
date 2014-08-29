package core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import core.StructDefine.Function;
import parser.ConvertFormula;
import parser.ExpParser;
import parser.FormulaParser;
import synthesis.SpecSynthesis;
import synthesis.basic.IOPair;
import synthesis.basic.Lval2Prog;
import synthesis.basic.ProgramAbstract;
import synthesis.basic.Result;
import synthesis.component.*;
import synthesis.util.Z3Util;
import file.SheetReader;

public class DetectRepairSmell {
	private SheetReader sheetReader;
	private StructDefine.Region cellArray;
	private ArrayList<StructDefine.Position> CA;
	private ArrayList<Entry<Formula, Integer>> SPEC;
	private ArrayList<StructDefine.R1C1Relative> IV;
	private ArrayList<StructDefine.Function> FUNC;
	private ArrayList<IOPair> IO;
	private ArrayList<ArithExpr> varsTable;
	private StructDefine.SmellAndRepair smellAndRepair;
	private Formula repairFormula;
	private Context ctx = Z3Util.getContext();
	
	private int state;	//-1-无法合成，1-已有公式，2-合成成功, 3-已修复
	
	public DetectRepairSmell(SheetReader sr, StructDefine.Region snip) throws Exception{
		this.sheetReader = sr;
		this.cellArray = snip;
		this.state = 0;
		this.CA = new ArrayList<StructDefine.Position>();
		this.smellAndRepair = new StructDefine.SmellAndRepair(snip);
		this.IV = new ArrayList<>();
		this.FUNC = new ArrayList<>();
		this.IO = new ArrayList<>();
		initCA(snip);
		initSPEC();
		initFUNC();
		initIO();
	}
	
	public boolean hasSmell() {
		if(FUNC.size() > 1) return true;
	    Formula lastFormula = null;
	    for(StructDefine.Position position : CA) {
    		if(sheetReader.getCells()[position.GetRow()][position.GetColumn()].getCellType() != Cell.CELL_TYPE_FORMULA)
    			return true;
    		else{
    			Formula formula = new Formula(position, sheetReader.getCells()[position.GetRow()][position.GetColumn()].getFormula());
    			if(lastFormula == null)
    				lastFormula = formula;
    			else {
    				if(lastFormula.getR1C1Formula().equals(formula.getR1C1Formula()))
    					continue;
					try {
						if(!FormulaParser.isEquivalent(formula, lastFormula, IV))
							return true;
					} catch (Z3Exception e) {
						e.printStackTrace();
					}
    			}
    		}
    	}
    	return false;
	}
	
	public boolean recovery() {
		if(state == 3) return true;
		try {
			repairFormula = recoveryFormulaPattern();
			if(repairFormula != null) {
				state = 1;
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean synthesize() {
		if(state == 3) return true;
		try {
			repairFormula = synthesizeFormulaPattern();
			if(repairFormula != null) {
				state = 2;
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		state = -1;
		return false;
	}

	public void repairSmell() throws Exception {
		if(state == 3 || repairFormula == null) return;
		
		int startRow = cellArray.GetTopLeft().GetRow(), endRow = cellArray.GetBottomRight().GetRow();
    	int startColumn = cellArray.GetTopLeft().GetColumn(), endColumn = cellArray.GetBottomRight().GetColumn();
    	for(int i = startRow ; i <= endRow ; i++)
    		for(int j = startColumn ; j <= endColumn ; j++) {
    			String advise = repairFormula.getA1Formula(new StructDefine.Position(i, j));
    			ExpParser expParser = new ExpParser(sheetReader, new StructDefine.Position(i, j));
    			double value = expParser.evaluate(repairFormula.getR1C1Formula());
    			String thisFormula = sheetReader.getCells()[i][j].getFormula();
    			if(thisFormula == null || (!advise.equals(thisFormula) && !FormulaParser.isEquivalent(repairFormula, new Formula(new StructDefine.Position(i, j) ,thisFormula), IV))) {
    				StructDefine.RepairAdvise ra = new StructDefine.RepairAdvise();
    				ra.SetFormula(advise);
    				ra.SetValue(value);
    				if((Double.parseDouble(sheetReader.getCells()[i][j].getValue()) - value > 0.000001) || (Double.parseDouble(sheetReader.getCells()[i][j].getValue()) - value < -0.000001))
    					ra.SetType(1);	//值错误
    				else 
    					ra.SetType(2);	//
    				smellAndRepair.AddRepairAdvise(new StructDefine.Position(i,j), ra);
    			}
    		}
    	state = 3;
	}
	
	public int getState() {
		return state;
	}
	
	public Formula getRepairFormula() {
		return repairFormula;
	}
	
	public StructDefine.SmellAndRepair getSmellAndRepair() {
		return smellAndRepair;
	}

	public StructDefine.Region getCellArray() {
		return cellArray;
	}
	
	private double getCellValue(StructDefine.Position position) {
		return sheetReader.getCellValueAt(position);
	}
	
	private double getCellValue(StructDefine.Position position, StructDefine.R1C1Relative r1c1Relative) {
		StructDefine.Position thisPosition = r1c1Relative.GetPosition(position);
		return sheetReader.getCellValueAt(thisPosition);
	}
	
	private void initCA(StructDefine.Region snip) {
		int rowStart = snip.GetTopLeft().GetRow(), rowEnd = snip.GetBottomRight().GetRow();
	    int columnStart = snip.GetTopLeft().GetColumn(), columnEnd = snip.GetBottomRight().GetColumn();
	    for(int i = rowStart ; i <= rowEnd ; i++)
	    	for(int j = columnStart ; j <= columnEnd ; j++)
	    		CA.add(new StructDefine.Position(i, j));
	}
	
	private void initSPEC() throws Z3Exception {
		Map<Formula, Integer> SPEC_Map = new HashMap<>();
		for(StructDefine.Position position : CA){
			StructDefine.Cell cell = sheetReader.getCells()[position.GetRow()][position.GetColumn()];
			if(cell.getCellType() == Cell.CELL_TYPE_FORMULA){
				Formula formula = new Formula(position, cell.getFormula());
				Iterator<Entry<Formula, Integer>> iter = SPEC_Map.entrySet().iterator();
				boolean existed = false;
				while (iter.hasNext()) {
					Entry<Formula, Integer> entry = iter.next();
					Formula key = entry.getKey();
					if(key.getR1C1Formula().equals(formula.getR1C1Formula())){
						entry.setValue(entry.getValue()+1);
						existed = true;
						break;
					}
				}
				if(!existed) SPEC_Map.put(formula, 1);

				ArrayList<StructDefine.R1C1Relative> input = formula.GetInputSet();
				for(StructDefine.R1C1Relative in : input){
					boolean contain = false;
					for(StructDefine.R1C1Relative tRelative : IV) {
						if(tRelative.equal(in)){
							contain = true;
							break;
						}
					}
					if(!contain) {
						IV.add(in);
					}
				}
			}	
		}
		Collections.sort(IV);
		
		this.SPEC = new ArrayList<>(SPEC_Map.entrySet());  
		Collections.sort(SPEC, new Comparator<Object>(){   
			@SuppressWarnings("unchecked")
			public int compare(Object e1, Object e2){   
				int v1 = ((Entry<Formula,Integer>) e1).getValue(); 
				int v2 = ((Entry<Formula,Integer>) e2).getValue();   
		        return v2-v1;        
		    }
		});
		
		this.varsTable = new ArrayList<>();
		for(int i = 0 ; i < IV.size() ; i++) {
			varsTable.add(ctx.mkIntConst("i"+i));
		}
	}
	
	private void initFUNC() throws Z3Exception {
		for(Entry<Formula, Integer> entry : SPEC){
			Formula formula = entry.getKey();
			String func = FormulaParser.preProcessFormula(formula.getR1C1Formula(), IV);
			if(FormulaParser.validFormula(func)) {
				boolean existed = false;
				for(StructDefine.Function function : FUNC) {
					String s = function.getFunc();
					if(FormulaParser.isEquivalent(func, s, IV)) existed = true;
				}
				if(!existed) {
					StructDefine.Function newFunction = new StructDefine.Function(formula);
					newFunction.setFunc(func);
					FUNC.add(newFunction);
				}
			}
		}
	}
	
	private void initIO() {
		for(StructDefine.Position position : CA) {
			boolean valid = true;
			int []inputs = new int[IV.size()];
			for(int i = 0 ; i < IV.size() ; i++) {
				StructDefine.R1C1Relative r1c1 = IV.get(i);
				double value = getCellValue(position, r1c1);
				if((int)value - value > 0.000001 || (int)value - value < -0.000001) valid = false;;
				inputs[i] = (int)value;
			}
			double output = getCellValue(position);
			if((int)output - output > 0.000001 || (int)output - output < -0.000001) valid = false;
			if(valid) IO.add(new IOPair(inputs, (int)output));
		}
		//System.err.println(IO.size());
	}
	
	private Formula recoveryFormulaPattern() throws Exception {
		if(SPEC.size() == 1) return SPEC.get(0).getKey();
		for(Entry<Formula, Integer> entry : SPEC){
			Formula form = entry.getKey();
			//if(form.GetInputSet().size() == IV.size()) {
				if(coverage(form) == CA.size()) {
					return form;
				}
			//}
		}
		return null;
	}
	
	private Formula synthesizeFormulaPattern() throws Exception {
		System.out.println("in---synthesizeFormulaPattern");
		ArrayList<ArrayList<StructDefine.Function>> groups = classify();
		System.out.println(groups.size());
		int pert = 0;
		StructDefine.Function F = null;
		for(ArrayList<StructDefine.Function> group : groups) {
			ArrayList<Component> comps = getComponents();
			ArrayList<IOPair> ioPairs = SpecSynthesis.doSpecSynthesis(IV, comps, group);
			Result result = SpecSynthesis.doIOSynthesis(IO, comps, IV, group, ioPairs);
			
			ProgramAbstract prog = new ProgramAbstract(comps, IV.size());
	        prog.init();
			String[] progs = Lval2Prog.tranform(result, prog, false);
			
			StructDefine.Function function = null;
			if (progs == null) {
				System.out.println("No solution! Components insufficient!");
			} else {
				String program = mergeProgs(progs);
				program = addSign(program);
		    	String A1Formula = convertToFormula(program, CA.get(0));
		    	Formula formula = new Formula(CA.get(0), A1Formula);
		    	function = new StructDefine.Function(formula);
		    	function.setFunc(program);
			}
			
			if(function == null) continue;
			int coverage = coverage(function.getFormula());
			if(coverage > pert) {
				pert = coverage;
				F = function;
			}
			if(coverage == IO.size()) break;
		}
		System.out.println("out---synthesizeFormulaPattern");
		if(F != null)
			return F.getFormula();
		else
			return null;
	}
	
	private ArrayList<ArrayList<StructDefine.Function>> classify() throws Z3Exception {
		System.out.println("in---classify");
		ArrayList<ArrayList<StructDefine.Function>> groups = new ArrayList<>();
		if(FUNC.size() == 0) return groups;
		int index = 0;
		while(index != -1) {
			ArrayList<StructDefine.Function> newGroup = new ArrayList<>();
			newGroup.add(FUNC.get(index));
			FUNC.get(index).setClassified();
			for(StructDefine.Function func : FUNC){
				boolean existed = false, compatible = true;
				for(StructDefine.Function function : newGroup) {
					if(func == function) existed = true;
					if(!isCompatible(function, func)) {
						compatible = false;
					}
				}
				if(existed || !compatible) continue;
				newGroup.add(func);
				func.setClassified();
			}
			groups.add(newGroup);
			
			index = -1;
			for(int i = 0 ; i < FUNC.size() ; i++) {
				if(!FUNC.get(i).classified()){
					index = i; break;
				}
			}
		}
		System.out.println("out---classify");
		return groups;
	}
	
	private int coverage(Formula formula) throws Exception{
		int coveredCells = 0;
		for(StructDefine.Position position : CA) {
			StructDefine.Cell cell = sheetReader.getCells()[position.GetRow()][position.GetColumn()];
			if(cell.getValueType() == 0){
				double cellValue = Double.parseDouble(cell.getValue());
				ExpParser eParser = new ExpParser(sheetReader, position);
				double formulaValue = eParser.evaluate(formula.getR1C1Formula());
				if(cellValue - formulaValue < 0.000001 && cellValue - formulaValue > -0.000001)
					coveredCells++;
			}
		}
		return coveredCells;
	}
	
	private boolean isCompatible(StructDefine.Function func1, StructDefine.Function func2) throws Z3Exception {
		System.out.println(func1.getFunc() + "," + func2.getFunc());
		ConvertFormula convertFormula = new ConvertFormula();
		ArithExpr iev1 = convertFormula.convertFormula(func1.getFunc(), varsTable, ctx);
		ArithExpr iev2 = convertFormula.convertFormula(func2.getFunc(), varsTable, ctx);
		
		Solver solver = ctx.mkSolver();
		solver.add(ctx.mkNot(ctx.mkEq(iev1, iev2)));
		
		for(int i = 0 ; i < IV.size() ; i++) {
			if(!func1.getFunc().contains("[i"+i+"]") || !func2.getFunc().contains("[i"+i+"]"))
				solver.add(ctx.mkEq(ctx.mkInt(0), varsTable.get(i)));
		}
		
		if (Status.SATISFIABLE == solver.check()) {
			return false;
		} else {
			return true;
		}
	}
	
	private ArrayList<Component> getComponents() {
		int []count = new int[3];
		for(StructDefine.Function function : FUNC) {
			String func = function.getFunc();
			count[0] += func.split("\\+").length-1;
			count[1] += func.split("\\-").length-1;
			count[2] += func.split("\\*").length-1;
		}
		
		for(StructDefine.Function function1 : FUNC) {
			for(StructDefine.Function function2 : FUNC) {
				if(!hasCommonCells(function1, function2)) {
					count[0]++;
					count[1]++;
					count[2]++;
				}
			}
		}
		
		ArrayList<Component> comps = new ArrayList<Component>();
		for(int i = 0 ; i < count[0] ; i++)
			comps.add(new PlusComponent());
		for(int i = 0 ; i < count[1] ; i++)
			comps.add(new MinusComponent());
		for(int i = 0 ; i < count[2] ; i++)
			comps.add(new MultComponent());
		
		return comps;
		
		/*ArrayList<Integer> constants = new ArrayList<>(); 
		boolean []exist = new boolean[5];
		for(StructDefine.Function function : FUNC) {
			String func = function.getFunc();
			if(func.contains("+")) exist[0] = true;
			if(func.contains("-")) exist[1] = true;
			if(func.contains("*")) exist[2] = true;
			ArrayList<Integer> consts = getConstantNum(func);
			for(Integer integer : consts) {
				if(!constants.contains(integer))
					constants.add(integer);
			}
		}
		ArrayList<Component> comps = new ArrayList<Component>();
		//for(int i = 0 ; i < constants.size() ; i++)
			//comps.add(new ConstantComponent());
		for(int i = 1 ; i < IV.size() + constants.size() ; i++) {
			if(exist[0]) {comps.add(new PlusComponent());}
			if(exist[1]) {comps.add(new MinusComponent());}
			if(exist[2]) {comps.add(new MultComponent());}
		}
		return comps;*/
	}
	
	private boolean hasCommonCells(Function function1, Function function2) {
		for(int i = 0 ; i < IV.size() ; i++) {
			if(function1.getFunc().contains("[i"+i+"]") && function2.getFunc().contains("[i"+i+"]"))
				return true;
		}
		return false;
	}
	
	/*private ArrayList<Integer> getConstantNum(String func) {
		ArrayList<Integer> constants = new ArrayList<>();
		String temp = func;
		for(int i = 0 ; i < IV.size() ; i++) 
			temp = temp.replaceAll("i"+i, "");
		int index = 0, length = temp.length();
		while(index < length) {
			int value = 0;
			while(index < length && !(temp.charAt(index) > '0' && temp.charAt(index) < '9'))
				index++;
			while(index < length && (temp.charAt(index) > '0' && temp.charAt(index) < '9')) {
				value = 10*value + (int)(temp.charAt(index)-'0');
				index++;
			}
			constants.add(value);
		}
		return constants;
	}*/

	private String mergeProgs(String[] progs) {
		int lineCount = progs.length;
		String program = null;
		int index = lineCount-1;
		for( ; index >= 0 ; index--) {
			if(progs[index].startsWith("ret")) {
				program = progs[index].substring(4);
				break;
			}
		}
		while(index > 0) {
			index--;
			String left = progs[index].replaceAll(" ", "").split("=")[0];
			String right = progs[index].replaceAll(" ", "").split("=")[1];
			if(right.contains("*")) {
				String[] tokens = right.split("\\*");
				right = "(" + tokens[0] + ")*(" + tokens[1] + ")";
			}
			else if(right.contains("/")) {
				String[] tokens = right.split("\\/");
				right = "(" + tokens[0] + ")/(" + tokens[1] + ")";
			}
			program = program.replaceAll(left, right);
		}
		return program;
	}
	
	private String addSign(String prog) {
		String program = "";
		int index = 0, length = prog.length();
		
		boolean in = false;
		while(index < length) {
			if(in) {
				if(prog.charAt(index) >= '0' && prog.charAt(index) <= '9')
					program = program + prog.charAt(index);
				else {
					program = program + "]" + prog.charAt(index);
					in = false;
				}
			}
			else {
				if(prog.charAt(index) == 'i') {
					program = program + "[i" ;
					in = true;
				}
				else
					program = program + prog.charAt(index);
			}
			index++;
		}
		if(in) program = program + "]";
		
		return program;
	}
	
	
	private String convertToFormula(String program, StructDefine.Position position) {
		String formula = program;
		System.out.println(formula);
		for(int i = 0 ; i < IV.size() ; i++) {
			String si = "\\[i" + i + "]";
			StructDefine.Position thisPosition = IV.get(i).GetPosition(position);
			char c = (char) ('A' + thisPosition.GetColumn());
			String sip = c + "" + (thisPosition.GetRow() + 1);
			formula = formula.replaceAll(si, sip);
		}
		return formula;
	}
}
