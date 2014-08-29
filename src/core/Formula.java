package core;

import java.util.ArrayList;
import java.util.Collections;

public class Formula {
	private ArrayList<StructDefine.R1C1Relative> InputSet;
	private String R1C1Formula;
	private boolean valid;

    private enum State {
        START, COLUMN, CELL, ONECHAR, SOMECHARS, LEFT, END
    }

    public Formula() {
        InputSet = new ArrayList<StructDefine.R1C1Relative>();
        valid = false;
    }

    public Formula(StructDefine.Position pos, String formula) {
    	while(formula.startsWith("+") || formula.startsWith("-"))
    		formula = formula.substring(1);
    	while(formula.contains("++"))
    		formula = formula.replaceAll("\\+\\+", "\\+");
    	while(formula.contains("--"))
    		formula = formula.replaceAll("--", "-");
    	while(formula.contains("(+"))
    		formula = formula.replaceAll("\\(\\+", "\\(");
    	while(formula.contains("(-"))
    		formula = formula.replaceAll("\\(-", "\\(");
    	formula = formula.replaceAll("%", "/100");
        InputSet = new ArrayList<StructDefine.R1C1Relative>();
        valid = true;
        R1C1Formula = convertToR1C1(pos, formula);
    }

    public String convertToR1C1(StructDefine.Position pos, String A1) {
    	A1 = A1.replaceAll(" ", "");
    	A1 = A1.toUpperCase();
    	//System.err.println(A1);
    	if(A1.startsWith("+")) A1 = A1.substring(1);
        int row = pos.GetRow(), column = pos.GetColumn();
        String result = "", checkValid = "";
        int length = A1.length(), index = 0;
        char lastChar = '#', lastTwoChar = '#';
        int lastInt = 0;
        State state = State.START;
        
        char thisChar = '#';
        while (state != State.END) {
            
            if (index == length) {
                state = State.END;
            } else {
                thisChar = A1.charAt(index++);
            }
            switch (state) {
                case START:
                    if (thisChar >= 'A' && thisChar <= 'Z') {
                        lastChar = thisChar;
                        checkValid += thisChar;
                        state = State.COLUMN;
                    } else {
                        result = result + thisChar;
                    }
                    break;
                case COLUMN:
                    if (thisChar >= '0' && thisChar <= '9') {
                    	checkValid = "";
                        lastInt = thisChar - '0';
                        //System.out.println(lastInt);
                        state = State.CELL;
                    } else if (thisChar >= 'A' && thisChar <= 'Z') {
                    	if(index == length) {
                    		valid = false;
                    		return "";
                    	}
                    	checkValid += thisChar;
                    	if(checkValid.length() > 3) {
                    		valid = false;
                    		return "";
                    	}
                    	char nextChar = A1.charAt(index);
                    	if(nextChar >= '0' && nextChar <= '9') {
                    		lastTwoChar = lastChar;
                    		lastChar = thisChar;
                    		//state = State.CELL;
                    		//index++;
                    	}
                    	else {
                    		result = result + lastChar;
                    		lastChar = thisChar;
                    	}
                    } else {
                    	checkValid = "";
                        result = result + lastChar + thisChar;
                        lastChar = '#';
                        state = State.START;
                    }
                    break;
                case CELL:
                	checkValid = "";
                    if (thisChar >= '0' && thisChar <= '9') {
                        lastInt = 10 * lastInt + (thisChar - '0');
                    } else {
                    	int columnInt = (lastChar - 'A') - column;
                    	if(lastTwoChar != '#') {
                    		columnInt += 26*(lastTwoChar - 'A' + 1);
                    		lastTwoChar = '#';
                    	}
                        int rowInt = lastInt - 1 - row;
                        StructDefine.R1C1Relative thisPosition = new StructDefine.R1C1Relative(rowInt, columnInt);
                    	if(!InputSet.contains(thisPosition))
                    		InputSet.add(thisPosition);
                        
                        result = result + "<R[" + rowInt + "]";
                        result = result + "C[" + columnInt + "]>";
                        
                    	if (thisChar >= 'A' && thisChar <= 'Z') {
                    		lastChar = thisChar;
                    		state = State.COLUMN;
                    	} else {
                    		result = result + thisChar;
                    		lastChar = '#';
                    		state = State.START;
                    	}
                    }
                    break;
                case END:
                    if (lastChar != '#') {
                        int columnInt = (lastChar - 'A') - column;
                        if(lastTwoChar != '#') {
                    		columnInt += 26*(lastTwoChar - 'A' + 1);
                    		lastTwoChar = '#';
                    	}
                        int rowInt = lastInt - 1 - row;
                        StructDefine.R1C1Relative thisPosition = new StructDefine.R1C1Relative(rowInt, columnInt);
                    	if(!InputSet.contains(thisPosition))
                    		InputSet.add(thisPosition);
                        
                        result = result + "<R[" + rowInt + "]";
                        result = result + "C[" + columnInt + "]>";
                    }
                    break;
                default:
                    break;
            }
        }
        result = addFuncSign(result);
        //处理函数中的：
        int sumStart = result.toLowerCase().indexOf("sum(");
        while(sumStart != -1) {
        	int sumEnd = result.indexOf("}", sumStart);
        	String sum = result.substring(sumStart, sumEnd);
        	String itemsString = sum.substring(4, sum.length()-1);
        	String[] items = itemsString.split(",");
        	for(String s : items) {
        		String[] s1 = s.split(":");
        		if(s1.length == 1) continue;
        		StructDefine.R1C1Relative start = StructDefine.R1C1Relative.convertStringToR1C1Relative(s1[0]);
        		StructDefine.R1C1Relative end = StructDefine.R1C1Relative.convertStringToR1C1Relative(s1[1]);
        		for(int i = start.GetRow() ; i <= end.GetRow() ; i++)
        			for(int j = start.GetColumn() ; j <=end.GetColumn() ; j++) {
        				if((i == start.GetRow() && j == start.GetColumn()) || (i == end.GetRow() && j == end.GetColumn()))
        					continue;
        				InputSet.add(new StructDefine.R1C1Relative(i, j));
        			}
        	}
        	sumStart = result.toLowerCase().indexOf("sum", sumEnd+1);
        }
        Collections.sort(InputSet);
        //System.err.println(result);
        return result;
    }
 
    public String convertToA1(StructDefine.Position pos, String R1C1) throws Exception {
    	String retString = "";
    	int length = R1C1.length(), index = 0;
    	State state = State.START;
    	String unhandled = "";
    	
    	while(state != State.END) {
    		char thisChar = '#';
            if (index == length) {
                state = State.END;
            } else {
                thisChar = R1C1.charAt(index++);
            }
            switch (state) {
			case START:
				if(thisChar == '<') {
					state = State.LEFT;
				}
				else if(thisChar != '{' && thisChar != '}'){
					retString += thisChar;
				}
				break;
			case LEFT:
				if(thisChar == '>') {
					StructDefine.Position position = transPos(unhandled, pos);
					int tempColumn = position.GetColumn();
					if(tempColumn < 26)
						retString += "" + (char)(tempColumn+'A');
					else {
						int firstNum = tempColumn/26;
						retString += "" + (char)(firstNum+'A'-1);
						retString += "" + (char)(tempColumn-firstNum*26+'A');
					}
					
					retString += "" + (position.GetRow()+1);
					state = State.START;
					unhandled = "";
				}
				else if(thisChar != '{' && thisChar != '}'){
					unhandled += thisChar;
				}
				break;
			default:
				break;
			}
    	}
    	return retString;
    }
    
    public ArrayList<StructDefine.R1C1Relative> GetInputSet() {
        return InputSet;
    }
    
    public String getR1C1Formula() {
    	return R1C1Formula;
    }
    
    public String getA1Formula(StructDefine.Position pos) throws Exception {
    	return convertToA1(pos, R1C1Formula);
    }
    
    public boolean getValid() {
    	return valid;
    }
    
    private StructDefine.Position transPos(String pos, StructDefine.Position position) throws Exception {
        //从sheet中取出该变量的值
    	String[] temp = pos.split("\\[|]");
    	int row = Integer.parseInt(temp[1]) + position.GetRow();
    	int column = Integer.parseInt(temp[3]) + position.GetColumn();
    	return new StructDefine.Position(row, column);
    }
    
    private String addFuncSign(String s) {
    	String result = "";
    	int length = s.length();
    	int index = 0;
    	int countOfLeft = 0;
    	char thisChar = '#';
    	String recordString = "", recordString2 = "";
    	State state = State.START;
    	
    	while (state != State.END) {
    		if (index == length) {
                state = State.END;
            } else {
                thisChar = s.charAt(index++);
                //System.out.println(thisChar);
            }
    		switch (state) {
            case START:
                if (isLetter(thisChar)) {
                    recordString += thisChar;
                    state = State.ONECHAR;
                } else {
                    result = result + thisChar;
                    recordString = "";
                }
                break;
            case ONECHAR:
                if (isLetter(thisChar)) {
                    recordString += thisChar;
                    state = State.SOMECHARS;
                } else {
                    result = result + recordString + thisChar;
                    recordString = "";
                    state = State.START;
                }
                break;
            case SOMECHARS:
                if (isLetter(thisChar)) {
                	recordString += thisChar;
                }
                else if (thisChar == '(') {
                	if(!recordString.equals("SUM")) valid = false;
                	recordString += thisChar;
                	state = State.LEFT;
                	countOfLeft++;
                }
                else {
                	result = result + recordString + thisChar;
                    recordString = "";
                    state = State.START;
                }
                break;
            case LEFT:
            	//recordString += thisChar;
            	recordString2 += thisChar;
            	if(thisChar == '(') {
            		countOfLeft++;	
            	}
            	if (thisChar == ')') {
            		countOfLeft--;
            		if(countOfLeft == 0) {
            			recordString2 = recordString2.substring(0, recordString2.length()-1);
            			result = result + "{" + recordString + addFuncSign(recordString2) +")}";
            			recordString = "";
            			recordString2 = "";
            			state = State.START;
            		}
            	}
            	break;
            case END:
                if (recordString != "") {
                    result = result + recordString;
                    recordString = "";
                }
                break;
            default:
                break;
    		}
    	}
    	return result;
    }
    
    private boolean isLetter(char c) {
    	if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
    		return true;
    	else {
			return false;
		}
    }

}
