package synthesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parser.ConvertFormula;
import synthesis.basic.IOPair;
import synthesis.basic.ProgramAbstract;
import synthesis.basic.ProgramInstance;
import synthesis.basic.Result;
import synthesis.component.Component;
import synthesis.io.IOSynthesis;
import synthesis.util.Z3Util;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import core.StructDefine;
import core.StructDefine.R1C1Relative;

public class SpecSynthesis extends IOSynthesis {	
	public static Result doIOSynthesis(ArrayList<IOPair> pairs, ArrayList<Component> comps, ArrayList<StructDefine.R1C1Relative> inputs,
			ArrayList<StructDefine.Function> specs, ArrayList<IOPair> ioPairs) throws Z3Exception {
        ProgramAbstract program = new ProgramAbstract(comps, inputs.size());
        program.init();

        ArrayList<IOPair> curPairs = ioPairs;
        Result res = null, lastRes = null;
        for (int i = 0; i < pairs.size(); i++) {
        	lastRes = res;
            res = IOSynthesis.generateProgram(program, curPairs);
            if (res == null) {
                curPairs.remove(curPairs.size() - 1);
                continue;
            } else {
                List<Integer> newInputs = generateDistinctProgram(program, curPairs, inputs, specs, false);
                if (newInputs == null) {
                        break;
                }
            }
            curPairs.add(pairs.get(i));
        }
        if(res == null) return lastRes;
        return res;
    }

  	public static ArrayList<IOPair> doSpecSynthesis(ArrayList<StructDefine.R1C1Relative> inputs, ArrayList<Component> comps, 
  			ArrayList<StructDefine.Function> specs) throws Z3Exception {
  		
        ProgramAbstract program = new ProgramAbstract(comps, inputs.size());
        program.init();

        ArrayList<IOPair> ioPairs = new ArrayList<IOPair>();
        ioPairs.add(generateIOPair(inputs, specs, null));

        while (true) {
            generateProgram(program, ioPairs);
            ArrayList<Integer> newInputs = generateDistinctProgram(program, ioPairs, inputs, specs, true);
            if (newInputs != null) {
                    ioPairs.add(generateIOPair(inputs, specs, newInputs));
            } else {
                    break;
            }
        }
        
        return ioPairs;
    }

    // generate distinct program
    public static ArrayList<Integer> generateDistinctProgram(ProgramAbstract program, ArrayList<IOPair> ioPairs, 
    		ArrayList<StructDefine.R1C1Relative> inputs, ArrayList<StructDefine.Function> specs, boolean inputConstraints) throws Z3Exception {
        Solver solver = ctx.mkSolver();

        // First program
        ProgramAbstract prog1 = new ProgramAbstract(program.components, inputs.size());
        prog1.init();
        addSynthesisConstraints(solver, prog1, ioPairs);

        // Second program
        ProgramAbstract prog2 = new ProgramAbstract(program.components, inputs.size());
        prog2.init();
        addSynthesisConstraints(solver, prog2, ioPairs);

        // function constraints for first program
        ProgramInstance pi1 = prog1.getInstance();
        pi1.init();
        addFuncConstraints(solver, pi1);

        // function constraints for second program
        ProgramInstance pi2 = prog2.getInstance();
        pi2.init();
        addFuncConstraints(solver, pi2);

        // input - output constraint for two program for different inputs
        for (int i = 0; i < inputs.size(); i++) {
            solver.add(ctx.mkEq(pi1.inputVars.get(i), pi2.inputVars.get(i)));
        }

        if (inputConstraints) {
            BoolExpr inputCons = getInputConstraints(ctx, pi1.inputVars, inputs, specs);
            solver.add(inputCons);
        }

        solver.add(ctx.mkNot(ctx.mkEq(pi1.outputVar, pi2.outputVar)));

        long start = System.currentTimeMillis();
        Params params = ctx.mkParams();
        params.add("timeout", 10000);
        solver.setParameters(params);
        Status status = solver.check();
        long end = System.currentTimeMillis();

        System.out.println("Time spent on generating distinct program: "
                        + (end - start) + "ms");

        if (status == Status.SATISFIABLE) {
            Model model = solver.getModel();

            ArrayList<Integer> newInputs = new ArrayList<Integer>();
            for (int i = 0; i < inputs.size(); i++) {
                    int input = ((IntNum) model.getConstInterp(pi1.inputVars.get(i))).getInt();
                    newInputs.add(input);
            }
            return newInputs;
        }
        return null;
    }

    public static BoolExpr getInputConstraints(Context context, List<Expr> inputVars, ArrayList<StructDefine.R1C1Relative> inputs,
    		ArrayList<StructDefine.Function> specs) throws Z3Exception {
        BoolExpr res = context.mkBool(false);
        for (StructDefine.Function spec : specs) {
                BoolExpr specRes = getSpecInputConstraints(context, inputVars, inputs, spec);
                res = context.mkOr(res, specRes);
        }
        return res;
    }

    public static BoolExpr getSpecInputConstraints(Context context, List<Expr> inputVars, ArrayList<StructDefine.R1C1Relative> inputs, 
    		StructDefine.Function spec) throws Z3Exception {
        BoolExpr res = context.mkBool(true);
        for (int i = 0; i < inputVars.size() ; i++) {
            if (!spec.getFunc().contains("i"+i)) {
            	res = context.mkAnd(res, context.mkEq(inputVars.get(i), context.mkInt(0)));
            }
        }
        return res;
    }

    public static IOPair generateIOPair(ArrayList<StructDefine.R1C1Relative> inputs,
    		ArrayList<StructDefine.Function> specs, List<Integer> concreteInputs) throws Z3Exception {
    	StructDefine.Function spec = selectSpec(inputs, specs, concreteInputs);

        Context context = Z3Util.getContext();
        Solver solver = context.mkSolver();

        Map<StructDefine.R1C1Relative, IntExpr> cell2Int = new HashMap<R1C1Relative, IntExpr>();
        List<Expr> allVars = new ArrayList<Expr>();
        ArrayList<ArithExpr> varTable= new ArrayList<ArithExpr>();
        for (int i = 0; i < inputs.size(); i++) {
            IntExpr var = context.mkIntConst("i" + i);
            cell2Int.put(inputs.get(i), var);
            varTable.add(var);
            allVars.add(var);
        }

        ConvertFormula convertFormula = new ConvertFormula();
        ArithExpr expr = convertFormula.convertFormula(spec.getFunc(), varTable, context);

        if (concreteInputs == null) {
            BoolExpr inputCons = getSpecInputConstraints(context, allVars, inputs, spec);
            solver.add(inputCons);
        } else {
            for (int i = 0; i < inputs.size(); i++) {
               	solver.add(context.mkEq(allVars.get(i), context.mkInt(concreteInputs.get(i))));
            }
        }

        IntExpr resExpr = context.mkIntConst("res");

        BoolExpr cond = context.mkEq(expr, resExpr);
        solver.add(cond);

        Params params = context.mkParams();
        params.add("timeout", 10000);
        solver.setParameters(params);
        Status status = solver.check();
        if (status == Status.SATISFIABLE) {
            Model model = solver.getModel();

            Integer[] newInputs = new Integer[inputs.size()];
            for (int i = 0; i < newInputs.length; i++) {
                    int input = ((IntNum) model.getConstInterp(allVars.get(i)))
                                    .getInt();
                    newInputs[i] = input;
            }
            int output = ((IntNum) model.getConstInterp(resExpr)).getInt();

            return new IOPair(newInputs, output);
        }
        return null;
    }

    public static StructDefine.Function selectSpec(ArrayList<StructDefine.R1C1Relative> inputs,
    		ArrayList<StructDefine.Function> specs, List<Integer> concreteInputs) {
        if (concreteInputs == null) {
        	return specs.get(0);
        }

        for (StructDefine.Function spec : specs) {
            boolean isComputed = true;
            for (int i = 0; i < inputs.size(); i++) {
                if (!spec.getFunc().contains("i"+i)) {
                    if (concreteInputs.get(i) != 0) {
                        isComputed = false;
                        break;
                    }
                }
            }
            if (isComputed) {
                    return spec;
            }
        }
        return null;
    }
}