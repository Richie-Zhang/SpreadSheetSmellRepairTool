package core;

import java.util.ArrayList;
import java.util.Stack;

import file.SheetReader;

public class ExtractCellArray {
	private SheetReader sheetReader;
	private Stack<StructDefine.Region> ChildSheet;
    private ArrayList<StructDefine.Region> Snippets;
    private ArrayList<StructDefine.Region> CellArrays;
    
    public ExtractCellArray(SheetReader sr) {
        this.sheetReader = sr;
        this.ChildSheet = new Stack<>();
        this.Snippets = new ArrayList<>();
        this.CellArrays = new ArrayList<>();
    }
    
    public void identifySnippets() {
    	Snippets.clear();
    	CellArrays.clear();
    	ChildSheet.push(new StructDefine.Region(new StructDefine.Position(0, 0), new StructDefine.Position(sheetReader.getRowCount()-1, sheetReader.getColumnCount()-1)));
        while (!ChildSheet.isEmpty()) {
            StructDefine.Region snippet = ChildSheet.pop();
            int rowStart = snippet.GetTopLeft().GetRow(), rowEnd = snippet.GetBottomRight().GetRow();
            int columnStart = snippet.GetTopLeft().GetColumn(), columnEnd = snippet.GetBottomRight().GetColumn();

            //Remove the border which is fence
            if (isFence(snippet, true, rowStart)) {
                if (rowEnd > rowStart) {
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart + 1, columnStart), new StructDefine.Position(rowEnd, columnEnd)));
                }
                continue;
            }
            if (isFence(snippet, true, rowEnd)) {
                if (rowEnd > rowStart) {
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart, columnStart), new StructDefine.Position(rowEnd - 1, columnEnd)));
                }
                continue;
            }
            if (isFence(snippet, false, columnStart)) {
                if (columnEnd > columnStart) {
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart, columnStart + 1), new StructDefine.Position(rowEnd, columnEnd)));
                }
                continue;
            }
            if (isFence(snippet, false, columnEnd)) {
                if (columnEnd > columnStart) {
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart, columnStart), new StructDefine.Position(rowEnd, columnEnd - 1)));
                }
                continue;
            }
            //find a row fence and divide the snippet
            boolean handleOne = false;
            for (int i = rowStart + 1; i <= rowEnd - 1; i++) {
                if (isFence(snippet, true, i)) {
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(i + 1, columnStart), new StructDefine.Position(rowEnd, columnEnd)));
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart, columnStart), new StructDefine.Position(i - 1, columnEnd)));
                    handleOne = true;
                    break;
                }
            }
            if (handleOne) continue;
            //find a column fence and devide the snippet
            for (int i = columnStart + 1; i <= columnEnd - 1; i++) {
                if (isFence(snippet, false, i)) {
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart, i + 1), new StructDefine.Position(rowEnd, columnEnd)));
                    ChildSheet.push(new StructDefine.Region(new StructDefine.Position(rowStart, columnStart), new StructDefine.Position(rowEnd, i - 1)));
                    handleOne = true;
                    break;
                }
            }
            if (handleOne) continue;
            //can't find a fence, so the snippet is pure
            Snippets.add(snippet);
        }
    }

    public ArrayList<StructDefine.Region> getSnippets() {
        return Snippets;
    }
    
    public void setSnippets(ArrayList<StructDefine.Region> snips) {
    	this.Snippets = snips;
    }
    
    public boolean addSnippet(StructDefine.Region snippet) {
    	if(isValidSnippet(snippet)) {
    		Snippets.add(snippet);
    		return true;
    	}
    	else 
    		return false;
    }

    public StructDefine.Region removeSnippet(int index) {
    	StructDefine.Region snip = Snippets.get(index);
        Snippets.remove(index);
        return snip;
    }

    public StructDefine.Region changeSnippet(int index, StructDefine.Region newSnippet) {
    	StructDefine.Region snip = Snippets.get(index);
    	Snippets.remove(index);
    	if(isValidSnippet(newSnippet)) {
    		Snippets.add(index, newSnippet);
    		return snip;
    	}
    	else {
    		Snippets.add(index, snip);
    		return null;
    	}	
    }

    public void extractCellArrays() {
        CellArrays.clear();
        for (StructDefine.Region snippet : Snippets) {
        	ArrayList<StructDefine.Region> ret = getCellArrayFromSnippet(snippet);
        	CellArrays.addAll(ret);
        }
    }
    
    public ArrayList<StructDefine.Region> getCellArrays() {
        return CellArrays;
    }
    
    public void setCellArrays(ArrayList<StructDefine.Region> cellArrays) {
    	this.CellArrays = cellArrays;
    }
    
    public boolean addCellArray(StructDefine.Region snippet) {
    	if(isValidCellArray(snippet)) {
    		CellArrays.add(snippet);
    		return true;
    	}
    	else
    		return false;
    }

    public StructDefine.Region removeCellArray(int index) {
    	StructDefine.Region snip = CellArrays.get(index);
        CellArrays.remove(index);
        return snip;
    }

    public StructDefine.Region changeCellArray(int index, StructDefine.Region newSnippet) {
    	StructDefine.Region snip = CellArrays.get(index);
	    CellArrays.remove(index);
    	if(isValidCellArray(newSnippet)) {
	        CellArrays.add(index, newSnippet);
	        return snip;
    	}
    	else {
    		CellArrays.add(index, snip);
    		return null;
    	}
    }
    
    private boolean isFence(StructDefine.Region snippet, boolean isRow, int index) {
        int rowStart = snippet.GetTopLeft().GetRow(), rowEnd = snippet.GetBottomRight().GetRow();
        int columnStart = snippet.GetTopLeft().GetColumn(), columnEnd = snippet.GetBottomRight().GetColumn();
        if (isRow) {
            for (int i = columnStart; i <= columnEnd; i++) {
                if (sheetReader.getCells()[index][i].getCellType() != -1 && sheetReader.getCells()[index][i].getValueType() == 0) {
                    return false;
                }
            }
            return true;
        } else {
            for (int i = rowStart; i <= rowEnd; i++) {
            	if (sheetReader.getCells()[i][index].getCellType() != -1 && sheetReader.getCells()[i][index].getValueType() == 0) {
            		return false;
            	}
            }
            return true;
        }
    }
    
    private ArrayList<StructDefine.Region> getCellArrayFromSnippet(StructDefine.Region snippet) {
    	int rowStart = snippet.GetTopLeft().GetRow(), rowEnd = snippet.GetBottomRight().GetRow();
        int columnStart = snippet.GetTopLeft().GetColumn(), columnEnd = snippet.GetBottomRight().GetColumn();
    	ArrayList<StructDefine.Region> ret = new ArrayList<>();
    	int [][]type = new int[rowEnd-rowStart+1][columnEnd-columnStart+1];
    	for(int i = rowStart ; i <= rowEnd ; i++)
    		for(int j = columnStart ; j <= columnEnd ; j++) {
    			type[i-rowStart][j-columnStart] = formulaInputType(new StructDefine.Position(i, j));
    		}
    	
    	for(int i = 0 ; i < rowEnd-rowStart+1 ; i++) {
    		int j = 0, k = 0;
    		while(j < columnEnd-columnStart+1) {
    			while(j < columnEnd-columnStart+1 && type[i][j] != 1 && type[i][j] != 0) j++;
    			if(j == columnEnd-columnStart+1) break;
    			k = j;
    			while(k < columnEnd-columnStart+1 && (type[i][k] == 1 || type[i][k] == 0)) k++;
    			if(k-j > 1) {
    				StructDefine.Region newRegin = new StructDefine.Region(new StructDefine.Position(rowStart+i, columnStart+j), new StructDefine.Position(rowStart+i, columnStart+k-1));
    				if(hasFormula(newRegin))
    					ret.add(pureCellArray(newRegin));
    			}
    			j = k;
    		}
    	}
    	
    	for(int j = 0 ; j < columnEnd-columnStart+1 ; j++) {
    		int i = 0, k = 0;
    		while(i < rowEnd-rowStart+1) {
    			while(i < rowEnd-rowStart+1 && type[i][j] != 2 && type[i][j] != 0 ) i++;
    			if(i == rowEnd-rowStart+1) break;
    			k = i;
    			while(k < rowEnd-rowStart+1 && (type[k][j] == 2 || type[k][j] == 0)) k++;
    			if(k-i > 1) {
    				StructDefine.Region newRegin = new StructDefine.Region(new StructDefine.Position(rowStart+i, columnStart+j), new StructDefine.Position(rowStart+k-1, columnStart+j));
    				if(hasFormula(newRegin))
    					ret.add(pureCellArray(newRegin));
    			}
    			i = k;
    		}
    	}
    	return ret;
    }
    
    private boolean hasFormula(StructDefine.Region snippet) {
    	int rowStart = snippet.GetTopLeft().GetRow(), rowEnd = snippet.GetBottomRight().GetRow();
        int columnStart = snippet.GetTopLeft().GetColumn(), columnEnd = snippet.GetBottomRight().GetColumn();
        for(int i = rowStart ; i <= rowEnd ; i++)
        	for(int j = columnStart ; j <= columnEnd ; j++) {
        		if(sheetReader.getCells()[i][j].getFormula() != null)
        			return true;
        	}
        return false;
    }
    
    private int formulaInputType(StructDefine.Position pos) {	//return the type of formula's input, 1-only the same column; 2-only the same row; 3-other
        String sFormula = sheetReader.getCells()[pos.GetRow()][pos.GetColumn()].getFormula();
        if(sFormula == null) return 0;
        Formula formula = new Formula(pos, sFormula);
        ArrayList<StructDefine.R1C1Relative> InputSet = formula.GetInputSet();
        boolean sameRow = false, sameColumn = false;
        for (StructDefine.R1C1Relative position : InputSet) {
            if (position.GetColumn() == 0 && position.GetRow() != 0) {
                sameColumn = true;
            }
            if (position.GetRow() == 0 && position.GetColumn() != 0) {
                sameRow = true;
            }
        }
        if (sameRow && !sameColumn) {
            return 2;
        }
        if (!sameRow && sameColumn) {
            return 1;
        }
        return 3;
    }

    private StructDefine.Region pureCellArray(StructDefine.Region snippet) {	//remove null cell in cell array¡®s border
    	int rowStart = snippet.GetTopLeft().GetRow(), rowEnd = snippet.GetBottomRight().GetRow();
        int columnStart = snippet.GetTopLeft().GetColumn(), columnEnd = snippet.GetBottomRight().GetColumn();
        if(rowStart == rowEnd) {
        	int columnStartNew = columnStart, columnEndNew = columnEnd;
        	while(columnStartNew <= columnEnd && sheetReader.getCells()[rowStart][columnStartNew].getValueType() != 0) columnStartNew ++;
        	while(columnEndNew >= columnStartNew && sheetReader.getCells()[rowStart][columnEndNew].getValueType() != 0) columnEndNew --;
        	if(columnStartNew <= columnEndNew) 
        		return new StructDefine.Region(new StructDefine.Position(rowStart, columnStartNew), new StructDefine.Position(rowEnd, columnEndNew));
        }
        else if(columnStart == columnEnd) {
        	int rowStartNew = rowStart, rowEndNew = rowEnd;
        	while(rowStartNew <= rowEnd && sheetReader.getCells()[rowStartNew][columnStart].getValueType() != 0) rowStartNew ++;
        	while(rowEndNew >= rowStartNew && sheetReader.getCells()[rowEndNew][columnStart].getValueType() != 0) rowEndNew --;
        	if(rowStartNew <= rowEndNew) 
        		return new StructDefine.Region(new StructDefine.Position(rowStartNew, columnStart), new StructDefine.Position(rowEndNew, columnEnd));
        }
        return null;
    }

    private boolean isValidSnippet(StructDefine.Region snippet) {
        int rowStart = snippet.GetTopLeft().GetRow(), rowEnd = snippet.GetBottomRight().GetRow();
        int columnStart = snippet.GetTopLeft().GetColumn(), columnEnd = snippet.GetBottomRight().GetColumn();
        for(StructDefine.Region snip : Snippets) {
        	if(reginOverRegion(snippet, snip)) return false;
        }
        for(int i = rowStart ; i <= rowEnd ; i++)
        	for(int j = columnStart ; j <= columnEnd ; j++) 
        		if(sheetReader.getCells()[i][j].getValueType() != 0)
        			return false;
        return true;
    }
    
    private boolean isValidCellArray(StructDefine.Region cellArray) {
    	int rowStart = cellArray.GetTopLeft().GetRow(), rowEnd = cellArray.GetBottomRight().GetRow();
        int columnStart = cellArray.GetTopLeft().GetColumn(), columnEnd = cellArray.GetBottomRight().GetColumn();
        if(rowStart != rowEnd && columnStart != columnEnd)
        	return false;
        for(StructDefine.Region snip : CellArrays) {
        	if(reginOverRegion(cellArray, snip)) return false;
        }
        for(int i = rowStart ; i <= rowEnd ; i++)
        	for(int j = columnStart ; j <= columnEnd ; j++) 
        		if(sheetReader.getCells()[i][j].getValueType() != 0)
        			return false;
        return true;
    }
    
    private boolean reginOverRegion(StructDefine.Region region1, StructDefine.Region region2) {
    	int rowStart1 = region1.GetTopLeft().GetRow(), rowEnd1 = region1.GetBottomRight().GetRow();
        int columnStart1 = region1.GetTopLeft().GetColumn(), columnEnd1 = region1.GetBottomRight().GetColumn();
        if(positionInRegion(region1.GetTopLeft(), region2) || positionInRegion(region1.GetBottomRight(), region2) ||
        		positionInRegion(new StructDefine.Position(rowStart1, columnEnd1), region2) || positionInRegion(new StructDefine.Position(rowEnd1, columnStart1), region2))
        	return true;
        
        int rowStart2 = region2.GetTopLeft().GetRow(), rowEnd2 = region2.GetBottomRight().GetRow();
        int columnStart2 = region2.GetTopLeft().GetColumn(), columnEnd2 = region2.GetBottomRight().GetColumn();
        if(positionInRegion(region2.GetTopLeft(), region1) || positionInRegion(region2.GetBottomRight(), region1) ||
        		positionInRegion(new StructDefine.Position(rowStart2, columnEnd2), region1) || positionInRegion(new StructDefine.Position(rowEnd2, columnStart2), region1))
        	return true;
        
        return false;
    }
    
    private boolean positionInRegion(StructDefine.Position pos, StructDefine.Region region) {
    	int rowStart = region.GetTopLeft().GetRow(), rowEnd = region.GetBottomRight().GetRow();
        int columnStart = region.GetTopLeft().GetColumn(), columnEnd = region.GetBottomRight().GetColumn();
        if(pos.GetRow() >= rowStart && pos.GetRow() <= rowEnd && pos.GetColumn() >= columnStart && pos.GetColumn() <= columnEnd)
        	return true;
        else
        	return false;
    }
}

