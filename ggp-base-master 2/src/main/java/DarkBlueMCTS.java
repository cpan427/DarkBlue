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


public class DarkBlueMCTS extends StateMachineGamer {
  Player p;
  Random rand = new Random();
  int BUFFER = 2000;

  @Override

  public StateMachine getInitialStateMachine() {
//	  return new CachedStateMachine(new ProverStateMachine());
	  return new CachedStateMachine(new PropNetStateMachine());
  }

  @Override
  public void stateMachineMetaGame(long timeout)
      throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

  }

  @Override
  public Move stateMachineSelectMove(long timeout)
      throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

    System.out.println("stateMachineSelectMove beginning");
    StateMachine machine = getStateMachine();
    MachineState state = getCurrentState();
    Role role = getRole();

    return bestMove(machine, role, state, timeout); //Improve as we go along
  }

  @Override
  public void stateMachineStop() {

  }

  @Override
  public void stateMachineAbort() {

  }

  @Override
  public void preview(Game g, long timeout) throws GamePreviewException {

  }

  @Override
  public String getName() {
    return "DarkBlueMCTScurrent";
 }


  public Move bestMove(StateMachine machine, Role role, MachineState state, long timeout)
        throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

	if (machine.getRoles().size() > 1) {
		return bestMoveMultiPlayer(machine, role, state, timeout);
	}

	return bestMoveSinglePlayer(machine, role, state, timeout);
  }

  public Move bestMoveMultiPlayer(StateMachine machine, Role role, MachineState state, long timeout)
        throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

    List<Move> moves = machine.getLegalMoves(state, role);
    Node root = new Node(machine, role, state, null, -1, -1);
    int i = 0;
    while(timeout - System.currentTimeMillis() > BUFFER) {
      i+=1;
      Node selected = root.select();
      int util = selected.getUtil();
      selected.backprop(util, -1, -1);
    }
    int index = root.bestMoveIndex();

    if (index >= 0) return moves.get(root.bestMoveIndex());

    return null;
  }

  public Move bestMoveSinglePlayer(StateMachine machine, Role role, MachineState state, long timeout)
        throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {

    List<Move> moves = machine.getLegalMoves(state, role);
    NodeSinglePlayer root = new NodeSinglePlayer(machine, role, state, null);

    int i = 0;
    while(timeout - System.currentTimeMillis() > BUFFER) {
      i+=1;
      NodeSinglePlayer selected = root.select();
      int util = selected.getUtil();
      selected.backprop(util);
    }

    int index = root.bestMoveIndex();
    if (index >= 0) return moves.get(root.bestMoveIndex());

    return null;

  }
}
