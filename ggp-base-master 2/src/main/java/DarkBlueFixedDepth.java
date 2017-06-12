import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.apps.player.Player;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DarkBlueFixedDepth extends StateMachineGamer {

	Player p;
	Role self;
	int selfID;
	Random rand = new Random();
	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		//return new CachedStateMachine(new ProverStateMachine());
		return new CachedStateMachine(new PropNetStateMachine2());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub


	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		System.out.println("stateMachineSelectMove beginning");
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		// System.currentTimeMillis();
		//List<Move> moves = machine.getLegalMoves(state, role);
		return bestMove(machine, role, state, timeout); //Improve as we go along
	}


	public Move bestMove(StateMachine machine, Role role, MachineState state, long timeout)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		self = role;
		selfID = self.hashCode();
		List<Move> moves = machine.getLegalMoves(state, role);
		Move move = moves.get(0);
		int score = 0;
		int level = 0;
		Move bestmove = move;

		while(timeout - System.currentTimeMillis() > 2000) {
			bestmove = move;
			for(int i = 0; i < moves.size(); i++) {
				Move nextMove = moves.get(i);
				int result = minscore(machine, role, nextMove, state, level, timeout);
				if(result == -1) break;
				if(result > score) {
					score = result;
					move = moves.get(i);
				}
			}

			level++;
		}

		return bestmove;
	}

	public int minscore(StateMachine machine, Role role, Move move, MachineState state, int level, long timeout)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {


		List<List<Move>> moves = machine.getLegalJointMoves(state, role, move);
		int score = 100;
		for(int i = 0; i < moves.size(); i++) {
			List<Move> curr = moves.get(i);
			MachineState newState = machine.getNextState(state, curr);
			int result = maxscore(machine, role, newState, level-1, timeout);
			if (result < score){
				score = result;
			}
		}
		return score;
	}

	public int maxscore(StateMachine machine, Role role, MachineState state, int level, long timeout)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		if(machine.isTerminal(state)) return machine.getGoal(state, role);
		if(timeout - System.currentTimeMillis() <= 2000) return -1;
		if(level < 0) {
			//return 0;
			//return mobility(machine, role, state);
			//return goalProximity(machine, role, state);
			//return focus(machine, role, state);
			return montecarlo(machine, role, state, 4);
			//return (int) (0.50*mobility(machine, role, state) +
					//0.50*goalProximity(machine,role,state) + 0.0*focus(machine, role, state));
		}
		List<Move> moves = machine.getLegalMoves(state, role);
		int score = 0;
		for(int i = 0; i < moves.size(); i++) {

			int result = minscore(machine, role, moves.get(i), state, level, timeout);
			if(result > score) {score = result;}
		}
		return score;
	}

	public int goalProximity(StateMachine machine, Role role, MachineState state) throws GoalDefinitionException {
		return machine.getGoal(state, role);
	}

	public int mobility(StateMachine machine, Role role, MachineState state) throws MoveDefinitionException {
		List<Move> moves = machine.getLegalMoves(state, role);
		List<Move> feasibles = machine.findActions(role);
		return(moves.size()/feasibles.size())*100;
	}

	public int focus(StateMachine machine, Role role, MachineState state) throws MoveDefinitionException {
		List<Move> moves = machine.getLegalMoves(state, role);
		List<Move> feasibles = machine.findActions(role);
		return(100 - (moves.size()/feasibles.size())*100);
	}

	public int montecarlo(StateMachine machine, Role role, MachineState state, int count) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		int total = 0;
		for(int i = 0; i < count; i++) {
			total = total + depthCharge(machine, role, state);
		}
		return total/count;
	}

	public int depthCharge(StateMachine machine, Role role, MachineState state) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if(machine.isTerminal(state)) return machine.getGoal(state, role);
		List<Role> roles = machine.getRoles();
		List<Move> moves = new ArrayList<Move>();
		for(int i = 0; i < roles.size(); i++) {
			List<Move> options = machine.getLegalMoves(state, roles.get(i));
			moves.add(i, options.get(rand.nextInt(options.size())));
		}

		//System.out.println(moves.toString());

		MachineState newState = machine.getNextState(state, moves);
		return depthCharge(machine, role, newState);
	}

//	public int opponentMobility(StateMachine machine, Role role, MachineState state) throws MoveDefinitionException {
//		List<List<Move>> opponentMoves = machine.getLegalJointMoves(state);
//		for(int i = 0; i < opponentMoves.size(); i++) {
//			List<Move> curr = opponentMoves.get(i);
//		}
//		return 0;
//	}

	//opponent mobility heuristic

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return "DarkBlueFixedDepth Player";
	}
}