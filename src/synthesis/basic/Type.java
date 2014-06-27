package synthesis.basic;

public class Type {
	public IOType ioType;
	public VarType varType;
	
	public Type() {
	}
	
	public Type(IOType ioType, VarType varType) {
		this.ioType = ioType;
		this.varType = varType;
	}
	
	public String toString() {
		return ioType + ":" + varType;
	}
}
