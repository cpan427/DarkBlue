import java.util.ArrayList;
import java.util.List;

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
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class DarkBlueAlphaBeta extends StateMachineGamer {

	Player p;

	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		return new CachedStateMachine(new ProverStateMachine());
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
		//List<Move> moves = machine.getLegalMoves(state, role);
		return bestMove(machine, role, state); //Improve as we go along
	}


	public static Move bestMove(StateMachine machine, Role role, MachineState state)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

		List<Move> moves = machine.getLegalMoves(state, role);
		Move move = moves.get(0);
		int score = 0;

		for(int i = 0; i < moves.size(); i++) {
			Move nextMove = moves.get(i);
			int result = minscore(machine, role, nextMove, state, 0, 100);
			if(result > score) {
				score = result;
				move = moves.get(i);
			}
		}
		return move;
	}


	public static int minscore(StateMachine machine, Role role, Move move, MachineState state, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		//for now, we're implementing mini max for a two player game. need to generalize to multiplayer games.
		//here, assume we have only 1 opponent or no opponents. So jointMoves should be of length 0 or 1.
		List<Role> roles = new ArrayList<Role>();
		roles = machine.getRoles();
		Role opp = null;
		//System.out.println(Arrays.toString(roles.toArray()));
		//System.out.println("This is my " + role.getName() + " id number " + role.hashCode());
		for(int i = 0; i < roles.size(); i++) {
			//System.out.println("This is " + roles.get(i).getName() + " id number " + roles.get(i).hashCode());
			if(roles.get(i).hashCode() != role.hashCode()){
				//System.out.println("In if statement");
				opp = roles.get(i);
				//roles.remove(i);
			}
			//System.out.println("This is roles: " + Arrays.toString(roles.toArray()));
		}

		/*
		List<List<Move>> jointMoves =  machine.getLegalJointMoves(state, role, move);
		List<Move> moves = new ArrayList<Move>();
		if (jointMoves.size() > 0){
			moves = jointMoves.get(1); //get moves for opponent
		}
//		List<Role> roles = machine.getRoles();
		else{
			//single player game -- get moves for self.
			moves = jointMoves.get(0);
		}
		*/
		//System.out.println("Opponent");
		//System.out.println(opp);
		List<Move> moves = machine.getLegalMoves(state, opp);
		//System.out.println("Moves size " + moves.size());
		for (int i = 0; i < moves.size(); i++)
		{
				List<Move> moveLocal = new ArrayList<Move>();
				//System.out.println("This is id of .get(0) " + machine.getRoles().get(0).hashCode());
				//System.out.println("This is my id " + role.hashCode());
				if (role.equals(machine.getRoles().get(0))){
					//System.out.println("Inside for loop and if statement");
					moveLocal.add(move);
					moveLocal.add(moves.get(i));

				}
				else{
					moveLocal.add(moves.get(i));
					moveLocal.add(move);
				}

				MachineState newState = machine.getNextState(state, moveLocal);
				int result = maxscore(machine, role, newState, alpha, beta);
				beta = Math.min(beta, result);
				if (beta <= alpha){
					return alpha;
				}
		}
		return beta;
	}


	public static int maxscore(StateMachine machine, Role role, MachineState state, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

		if(machine.isTerminal(state)) return machine.getGoal(state, role);
		List<Move> moves = machine.getLegalMoves(state, role);
		for(int i = 0; i < moves.size(); i++) {

			int result = minscore(machine, role, moves.get(i), state, alpha, beta);
			alpha = Math.max(alpha, result);
			if(alpha >= beta) {return beta;}
		}
		return alpha;
	}

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
		// TODO Auto-generated method stub
		return "DarkBlueAlphaBeta Player";
	}

}
