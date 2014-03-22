package synthesis;

public class IOPair {
	public int[] inputs;
	public int output;
	
	public IOPair() {
	}
	
	public IOPair(Integer[] inputs, int output) {
		this.inputs = new int[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			this.inputs[i] = inputs[i];
		}
		this.output = output;
	}
	
	public IOPair(int[] inputs, int output) {
		this.inputs = inputs;
		this.output = output;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IOPair) {
			IOPair pair = (IOPair)obj;
			for (int i=0; i<inputs.length; i++) {
				if (inputs[i] != pair.inputs[i]) {
					return false;
				}
			}
			if (output != pair.output) {
				return false;
			}
			
			return true;
		}
		return false;
	}
}
