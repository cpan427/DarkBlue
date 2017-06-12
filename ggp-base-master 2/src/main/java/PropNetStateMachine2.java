import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class PropNetStateMachine2 extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {

        	//Subgoal reordering -- simplest subgoals first
        	//Analyze GDL sentence and reorder them

            propNet = OptimizingPropNetFactory.create(description); //Creates the data structure
            roles = propNet.getRoles(); // Get the players in the game
            //ordering = getOrdering();



        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void markbases(MachineState state) {
    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
    	Set<GdlSentence> currStateMarkings = state.getContents();
    	Set<GdlSentence> baseKeys = bases.keySet();
    	for(GdlSentence key : baseKeys) {
    		if(currStateMarkings.contains(key)){
    			bases.get(key).setValue(true);
    		}
    		else {
    			bases.get(key).setValue(false);
    		}
    	}
    }

    private void markactions(MachineState state, Map<GdlSentence, Proposition> inputs, List<Move> moves){
    	for(Proposition input : inputs.values()){
    		input.setValue(false);
    	}
    	List<GdlSentence> nextMarkings = toDoes(moves);
    	for(int i = 0; i < nextMarkings.size(); i++) {
    		GdlSentence marking = nextMarkings.get(i);
    		inputs.get(marking).setValue(true);
    	}
    }

    private void clearpropnet(MachineState state){
    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
    	for(GdlSentence base : bases.keySet()){
    		bases.get(base).setValue(false);
    	}
    }

    private boolean computeViews(Component prop) throws Exception{

    	//Cases where the passed component is a proposition
    	if(prop instanceof Proposition) {
    		return markProposition((Proposition) prop);
    	}

    	//If a conjunction, negation, transition, or constant
    	Set<Component> sources = prop.getInputs();
    	if(prop instanceof And){
    		for(Component source : sources){
    			if(computeViews(source)) return true;
    		}
    		return false;
    	} else if(prop instanceof Or){
    		for(Component source : sources) {
    			if(!computeViews(source)) return false;
    		}
    		return true;
    	} else if(prop instanceof Not){
    		if(sources.size() != 1) throw new Exception("bad input");
    		return !computeViews(sources.iterator().next());

    	} else if(prop instanceof Constant){
    		return prop.getValue();

    	} else if(prop instanceof Transition){
    		return prop.getValue();
    	}

    	//If nothing else, its a view.
    	if(sources.size() == 1) return computeViews(prop.getSingleInput());
    	return false;
    }

    //Dealing with marking a proposition.
    private boolean markProposition(Proposition prop) throws InterruptedException {
    	if(propNet.getBasePropositions().containsValue((Proposition) prop)) return prop.getValue();
		if(propNet.getInputPropositions().containsValue((Proposition) prop)) return prop.getValue();
		if(propNet.getInitProposition().equals((Proposition) prop)) return prop.getValue();

		return false;
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	markbases(state);
    	try {
			return computeViews(propNet.getTerminalProposition());
		} catch (Exception e) {
			System.out.println("Compute Views must be changed!");
			e.printStackTrace();
			return false;
		}
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {

        markbases(state);
        Set<Proposition> goals = propNet.getGoalPropositions().get(role);
        for(Proposition goal : goals) {
        	try {
				if(computeViews(goal)) {
					return getGoalValue(goal);
				}
			} catch (Exception e) {
				System.out.println("Something wrong with computing view");
				e.printStackTrace();
			}
        }

        return 0;
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	propNet.getInitProposition().setValue(true);
        Set<GdlSentence> contents = new HashSet<GdlSentence>();

        for(Proposition p : propNet.getBasePropositions().values()) {
        	if(p.getSingleInput().getSingleInput().getValue()) contents.add(p.getName());
        }

        MachineState state = new MachineState(contents);
        propNet.getInitProposition().setValue(false);
        return state;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {

    	Set<Proposition> props = propNet.getLegalPropositions().get(role);
        List<Move> actions = new ArrayList<Move>();
        for(Proposition prop : props){
        	actions.add(getMoveFromProposition(prop));
        }

        return actions;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) {
    	markbases(state);
    	Set<Proposition> legalprops = propNet.getLegalPropositions().get(role);
    	List<Move> actions = new ArrayList<Move>();

    	for(Proposition prop : legalprops) {
    		try {
				if(computeViews(prop)){
					actions.add(getMoveFromProposition(prop));
					System.out.println(actions.toString());
				}
			} catch (Exception e) {
				System.out.print("Some sort of error getting moves");
				e.printStackTrace();
			}
    	}

    	if(actions.size() == 0) System.out.println("No moves found.");
        return actions;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {

    	//1. Mark input propositions using the given move.
    	Map<GdlSentence, Proposition> inputs = propNet.getInputPropositions();
    	markactions(state, inputs, moves);

    	//2. Mark base propositions and retrieve updated nodes
    	markbases(state);
    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();

    	//3. Iterate over the bases to check each base and collect those that are true.
    	Set<GdlSentence> nexts = new HashSet<GdlSentence>();
    	for(GdlSentence base : bases.keySet()) {
    		try {
				if(computeViews(bases.get(base).getSingleInput().getSingleInput())) nexts.add(base);

			} catch (Exception e) {
				System.out.println("Can't find the next state from base proposition");
				e.printStackTrace();
			}
    	}

    	MachineState nextState = new MachineState(nexts);
    	return nextState;
    }

    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    @Override
	public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.


        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }
}