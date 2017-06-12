import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


public class MyRunnable implements Callable{
	private StateMachine machine;
	private Role role;
	private MachineState oriState;
	private Random rand;
	private Semaphore depthDiveChecker;

	MyRunnable(StateMachine machine, Role role, MachineState state, Random rand, Semaphore depthDiveChecker){
		this.machine = machine;
		this.role = role;
		this.oriState = state;
		this.rand = rand;
		this.depthDiveChecker = depthDiveChecker;
	}

	private int depthCharge(MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

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
		return depthCharge(newState);
	}

	@Override
	public String call() {
		try {
			System.out.print("Started");
			int temp = depthCharge(oriState);
			depthDiveChecker.release();
			System.out.println("Ended");
			return String.valueOf(temp);

		} catch (MoveDefinitionException | GoalDefinitionException | TransitionDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return String.valueOf(0);
		}

	}

}
