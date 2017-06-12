import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NodeSinglePlayer {
	private Role role;
	private int id;
	private MachineState state;
	private StateMachine machine;
	private NodeSinglePlayer parent;
	private int visits;
	private int Up; // utilty of node
	private List<Move> moves;
	private ArrayList<NodeSinglePlayer> children;
	static Random rand = new Random();

	NodeSinglePlayer (StateMachine machine, Role role, MachineState state, NodeSinglePlayer parent) throws MoveDefinitionException{
		this.role = role;
		this.id = role.hashCode();
		this.state = state;
		this.machine = machine;
		this.parent = parent;
		this.visits = 0;
		this.Up = 0;
		this.children = new ArrayList<NodeSinglePlayer>();

	try	{
		this.moves = machine.getLegalMoves(state, role);
	}
	 catch(Exception e){
		 this.moves = new ArrayList<Move>();
	 	}
	}

	//never expand a node that's a terminal state.getNextState will still give states but will have errors. So if at terminal state don't expand more nodes.
	public NodeSinglePlayer select() throws TransitionDefinitionException, MoveDefinitionException{//returns a NodeSinglePlayer
		if(this.machine.isTerminal(this.state)) {
		  // Don't expand terminal nodes
		  return this;
		}

		if (this.children.size() < this.moves.size()){
		    Move curr = moves.get(this.children.size());
		    ArrayList<Move> mov = new ArrayList<Move>();
		    mov.add(curr);
		    MachineState newState = machine.getNextState(this.state, mov);

		    NodeSinglePlayer child = new NodeSinglePlayer(this.machine, this.role, newState, this);

		    this.children.add(child);
		    return child;

		}
		// all children have been generated -- get best child
		int index = this.bestMoveIndex();
		if (index == -1){
			return null;
		}

		return this.children.get(index).select();
	}

	public void backprop(int util){ //returns void:
		this.visits++;
		this.Up = this.Up + util;
		if (this.parent != null){
		  this.parent.backprop(util);
		}
	}

	public int getUtil() throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException{ // returns int
		if(this.machine.isTerminal(state)){
			return this.machine.getGoal(this.state, this.role);
		}

		else{
			    try {
			        return montecarlo(this.machine, this.role, this.state, 1);
			     } catch (StackOverflowError e) {
			        return 0;
			     }

		}

					//return this.machine.getGoal(this.state, this.role); //depth charge -- goals only defined for terminal states, but here want goal of non terminal sate

	}




	public static int montecarlo(StateMachine machine, Role role, MachineState state, int count) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

		int total = 0;

		for(int i = 0; i < count; i++) {

			total = total + depthCharge(machine, role, state);

		}

		return total/count;

	}




	public static int depthCharge(StateMachine machine, Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {


		if(machine.isTerminal(state)) return machine.getGoal(state, role);

		List<Role> roles = machine.getRoles();

		List<Move> moves = new ArrayList<Move>();

		for(int i = 0; i < roles.size(); i++) {

			List<Move> options = machine.getLegalMoves(state, roles.get(i));

			moves.add(i, options.get(rand.nextInt(options.size())));

		}

		MachineState newState = machine.getNextState(state, moves);

		return depthCharge(machine, role, newState);

	}

	public int bestMoveIndex(){// returns int:

		double bestVal = -1;
		int index = -1;

		for (int i = 0; i < this.children.size(); i++) {

		  double val = ((double)this.children.get(i).Up)/this.children.get(i).visits + Math.sqrt(2*Math.log(this.visits)/this.children.get(i).visits);

		  if (val > bestVal) {

		    bestVal = val;

		    index = i;

		  }

		}

		return index;

	}
}
