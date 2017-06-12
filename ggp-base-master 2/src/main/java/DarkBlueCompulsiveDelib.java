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

public class DarkBlueCompulsiveDelib extends StateMachineGamer {

	Player p;

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		return bestMove(machine, role, state);
	}

	public static Move bestMove(StateMachine machine, Role role, MachineState state)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		List<Move> moves = machine.getLegalMoves(state, role);
		Move move = moves.get(0);
		int score = 0;
		List<MachineState> states = machine.getNextStates(state);
		for(int i = 0; i < states.size(); i++) {
			MachineState next_state = states.get(i);
			int result = maxscore(machine, role, next_state);
			if(result == 100) return moves.get(i);
			if(result > score) {
				score = result;
				move = moves.get(i);
			}
		}
		return move;
	}

	public static int maxscore(StateMachine machine, Role role, MachineState state)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if(machine.isTerminal(state)) return machine.getGoal(state, role);
		List<MachineState> states = machine.getNextStates(state);
		int score = 0;
		for(int i = 0; i < states.size(); i++) {
			int result = maxscore(machine, role, states.get(i));
			if(result > score) score = result;
		}
		return score;
	}

	@Override
	public void stateMachineStop() {
		// Do nothing
	}

	@Override
	public void stateMachineAbort() {
		// Do nothing
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Do nothing
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "DarkBlueCompulsiveDeliberation";
	}

}
