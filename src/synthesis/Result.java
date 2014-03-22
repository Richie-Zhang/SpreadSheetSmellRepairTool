package synthesis;

import java.util.List;

public class Result {
	public List<Integer> locs;
	public List<Integer> cons;
	
	public Result(List<Integer> locs, List<Integer> cons) {
		this.locs = locs;
		this.cons = cons;
	}
}
