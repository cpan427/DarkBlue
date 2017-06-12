import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NodeThreaded {
	private Role role;
	private int id;
	private MachineState state;
	private StateMachine machine;
	private Node parent;
	private int childIndex;
	private int grandChildIndex;
	private int visits;
	private List<Integer> Up;
	private List<Integer> Vp;
	private List<ArrayList<Integer>> Uo;
	private List<ArrayList<Integer>> Vo;
	private List<List<List<Move>>> moves;
	private List<ArrayList<Node>> grandChildren;
	private int child;
	private static int count = 3;
	private static ExecutorService threadPool = Executors.newCachedThreadPool(); /////new
	private static Semaphore depthDiveChecker = new Semaphore(count);
	private static int total = 0;

	static //private int child;
	Random rand = new Random();

	NodeThreaded (StateMachine machine, Role role, MachineState state, Node parent, int myChildIndex, int myGrandchildIndex) throws MoveDefinitionException{
	 this.role = role;
	 this.id = role.hashCode();
	 this.state = state;
	 this.machine = machine;
	 this.parent = parent;
	 this.childIndex = myChildIndex;
	 this.grandChildIndex = myGrandchildIndex;
	 this.visits = 0; // = sum of elements of V_p
	 this.Up = new ArrayList<Integer>(); // U_p = utilties of blue child nodes
	 this.Vp = new ArrayList<Integer>(); // V_p = visits made to blue child nodes
	 this.Uo = new ArrayList<ArrayList<Integer>>(); // U_o = utilties of red grandchild nodes
	 this.Vo = new ArrayList<ArrayList<Integer>>(); // V_o = visits made to red grandchild nodes
	 this.moves = new ArrayList<List<List<Move>>>();
	 this.grandChildren = new ArrayList<ArrayList<Node>>();
	 this.child = 0;

	 List<Move> movesp = machine.getLegalMoves(state, role);  
	 for (int i = 0; i < movesp.size(); i++) {
		 List<List<Move>> movesi;
		 try{
			 movesi = machine.getLegalJointMoves(state, role, movesp.get(i));

		 }
		 catch(Exception e){

			 continue;
		 }

		 this.moves.add(movesi);

	    Vp.add(0);
	    Up.add(0);
	    ArrayList<Integer> Voi = new ArrayList<Integer>();
	    ArrayList<Integer> Uoi = new ArrayList<Integer>();
	    for (int j = 0; j < movesi.size(); j++) {
	      Voi.add(0);
	      Uoi.add(0);
	    }
	    Vo.add(Voi);
	    Uo.add(Uoi);
	    grandChildren.add(new ArrayList<Node>());
	 }
	}

	//never expand a node that's a terminal state.getNextState will still give states but will have errors. So if at terminal state don't expand more nodes.
	public Node select() throws TransitionDefinitionException, MoveDefinitionException{//returns a Node

		if(this.machine.isTerminal(this.state)){ //don't expand terminal nodes
			return this;
		}
		while(this.child < moves.size()){
		  if(grandChildren.get(this.child).size() == moves.get(this.child).size()){
		    this.child++;
		  }
		  else{
		    List<Move> curr = moves.get(this.child).get(grandChildren.get(this.child).size());
		    MachineState newState = machine.getNextState(this.state, curr);
		    Node grandChild = new Node(this.machine, this.role, newState, this, this.child, grandChildren.get(this.child).size());
		    grandChildren.get(this.child).add(grandChild);
		    return grandChild;
		  }
		}

		// all grandchildren have been generated -- get best blue node , store index in child index
		double bestVal = -1;
		int childIndex = -1;
		for (int i = 0; i < Up.size(); i++) {
		  double val = ((double)Up.get(i))/Vp.get(i) + Math.sqrt(2*Math.log(this.visits)/Vp.get(i));
		  if (val > bestVal) { //for blue node
		    bestVal = val;
		    childIndex  = i;
		  }
		}

		if(childIndex == -1) {return null;}

		int grandChildIndex = -1;
		bestVal = 1e50;
		int childVisits = Vp.get(childIndex);
		for (int i = 0; i < Uo.get(childIndex).size(); i++) {
		  double val = ((double)Uo.get(childIndex).get(i))/Vo.get(childIndex).get(i) + Math.sqrt(2*Math.log(childVisits)/Vo.get(childIndex).get(i));
		  if (val < bestVal) {//for red node
		    bestVal = val;
		    grandChildIndex = i;
		  }
		}

			if (grandChildIndex == -1){
				return null;
			}
			return grandChildren.get(childIndex).get(grandChildIndex).select();
	}

	public void backprop(int util, int cIndex, int gcIndex){ //returns void:
		if (cIndex >= 0){
		  Vp.set(cIndex, Vp.get(cIndex) + 1);
		  Up.set(cIndex, Up.get(cIndex) + util);
		  this.visits++;
		  if (gcIndex >= 0){
		    Vo.get(cIndex).set(gcIndex, Vo.get(cIndex).get(gcIndex) + 1);
		    Uo.get(cIndex).set(gcIndex, Uo.get(cIndex).get(gcIndex) + util);

		  }
		}
		if (parent != null){
		  parent.backprop(util, this.childIndex, this.grandChildIndex);
		}

	}

	public int getUtil() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{ // returns int
		if(this.machine.isTerminal(state)){
			return this.machine.getGoal(this.state, this.role);
		}
		else{
			try{
				return montecarlo(this.machine, this.role, this.state, count);
			}
			catch(StackOverflowError e){
				return 0;
			}
		}
	}

	/*synchronized static void addResult(int addition){
		total += addition;
	}

	private static void innerLoopLogic(StateMachine machine, Role role, MachineState state){
		try{
        	int temp = depthCharge(machine, role, state);
        	addResult(temp);
        }catch(Exception e){
			System.out.println(e);
        }
	}*/

	public static int montecarlo(StateMachine machine, Role role, MachineState state, int count) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		int total = 0;
		System.out.println("Here");
		ArrayList<Future<String>> results = new ArrayList<Future<String>>();
		for(int i = 0; i < count; i++) {
			try {
				System.out.print("Waiting....");
				depthDiveChecker.acquire(1);
				results.add(threadPool.submit(new MyRunnable(machine, role, state, rand, depthDiveChecker)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//total = total + depthCharge(machine, role, state);
		}
		try {
			System.out.println("Waiting.... for them to finish");
			depthDiveChecker.acquire(count);
			System.out.println("Passed");
			for (int i = 0; i < count ; i++){
				Integer curr = Integer.parseInt(results.get(i).get());
				System.out.println(curr);
				total += curr;
			}
			depthDiveChecker.release(5);
		} catch (InterruptedException | NumberFormatException | ExecutionException e) {
			// TODO Auto-generated catch block
			depthDiveChecker.release(5);
			e.printStackTrace();
		}
		return total/count; //Isn't this a double?????
	}

	static int it=0;
	public static int depthCharge(StateMachine machine, Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

		if(machine.isTerminal(state)) return machine.getGoal(state, role);
		List<Role> roles = machine.getRoles();
		List<Move> moves = new ArrayList<Move>();
		for(int i = 0; i < roles.size(); i++) {
			List<Move> options = machine.getLegalMoves(state, roles.get(i));
			if(options.size() == 0){
				return 0;
			}
			moves.add(i, options.get(rand.nextInt(options.size())));
		}

		MachineState newState = machine.getNextState(state, moves);
		System.out.println(it++);
		return depthCharge(machine, role, newState);
	}
	public int bestMoveIndex(){// returns int:
		double bestVal = -1;
		int childIndex = -1;
		for (int i = 0; i < Up.size(); i++) {
		  double val = ((double)Up.get(i))/Vp.get(i) + Math.sqrt(2*Math.log(this.visits)/Vp.get(i));
		  if (val > bestVal) {
		    bestVal = val;
		    childIndex  = i;
		  }
		}
		return childIndex;
	}



}
