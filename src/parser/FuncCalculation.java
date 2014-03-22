package parser;

import core.StructDefine;
import file.SheetReader;

public class FuncCalculation {
	final double MAXVALUE = 1.7976931348623157E308;
	final double MINVALUE = 4.9E-324;
	
	private SheetReader sheetReader;
	private StructDefine.Position position;
	private String function;
	private double result;
	
	public FuncCalculation(String function, SheetReader sheetReader, StructDefine.Position position) {
		this.function = function;
		this.sheetReader = sheetReader;
		this.position = position;
		result = 0;
	}
	
	public double getResult() throws Exception {
		parseFunction();
		return result;
	}
	
    private void parseFunction() throws Exception{
    	if(function.toUpperCase().startsWith("SUM") || function.toUpperCase().startsWith("AVERAGE") || 
    			function.toUpperCase().startsWith("MAX") || function.toUpperCase().startsWith("MIN")) {
    		int countOfVar = 0;
    		double max = MINVALUE, min = MAXVALUE, sum = 0;
    		int startIndex = function.toUpperCase().startsWith("AVERAGE") ? 8 : 4;
    		String s1 = function.substring(startIndex, function.length()-1);
    		String []s2 = s1.split(",");
    		
    		for(String s3 : s2) {
    			String []s4 = s3.split(":");
    			if(s4.length == 1){	//
    				ExpParser ep = new ExpParser(sheetReader, position);
    				countOfVar++;
    				double thisValue = ep.evaluate(s4[0]);
    				sum += thisValue;
    				if(thisValue > max) max = thisValue;
    				if(thisValue < min) min = thisValue;
    			}
    			else{
    				StructDefine.Position startPosition = TransPos(s4[0]), endPosition = TransPos(s4[1]);
    				int rowStart = startPosition.GetRow(), columnStart = startPosition.GetColumn();
    				int rowEnd = endPosition.GetRow(), columnEnd = endPosition.GetColumn();
    				for(int i = rowStart ; i <= rowEnd ; i++) {
    					for(int j = columnStart ; j <= columnEnd ; j++){
							if(sheetReader.getCells()[i][j].getValueType() == 0) {
								countOfVar++;
								double thisValue = Double.parseDouble(sheetReader.getCells()[i][j].getValue());
								sum += thisValue;
			    				if(thisValue > max) max = thisValue;
			    				if(thisValue < min) min = thisValue;
							}
							else {
								//TODO
							}
    					}
    				}
    			}
    		}
    		if(function.toUpperCase().startsWith("SUM"))
    			result = sum;
    		if(function.toUpperCase().startsWith("AVERAGE"))
    			result = sum/countOfVar;
    		if(function.toUpperCase().startsWith("MAX"))
    			result = max;
    		if(function.toUpperCase().startsWith("MIN"))
    			result = min;
    	}
    	else if(function.toUpperCase().startsWith("ABS")) {
    		String s1 = function.substring(4, function.length()-1);
    		ExpParser ep = new ExpParser(sheetReader, position);
    		double thisValue = ep.evaluate(s1);
    		if(thisValue >= 0) result = thisValue;
    		else result = -thisValue;
    	}
    }

	private StructDefine.Position TransPos(String pos) throws Exception {
        //从sheet中取出该变量的值
    	String[] temp = pos.split("\\[|]");
    	int row = Integer.parseInt(temp[1]) + position.GetRow();
    	int column = Integer.parseInt(temp[3]) + position.GetColumn();
    	return new StructDefine.Position(row, column);
    }
}
