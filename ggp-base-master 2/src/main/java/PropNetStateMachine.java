import java.util.ArrayList;
import java.util.HashSet;
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
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;





@SuppressWarnings("unused")

public class PropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    //private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;



    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override

    public void initialize(List<Gdl> description) {
    	System.out.print(description);
        try {

            propNet = OptimizingPropNetFactory.create(description);

            roles = propNet.getRoles();

//            ordering = getOrdering();

        } catch (InterruptedException e) {
        	System.out.print(e);
            throw new RuntimeException(e);

        }

    }



    private void markBasesFromState(MachineState state) {
    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
	    Set<GdlSentence> contents = state.getContents();
	    for(Map.Entry<GdlSentence, Proposition> base : bases.entrySet()){
	    if(contents.contains(base.getKey())) {
	    	base.getValue().setValue(true);
	    }

    else {

    base.getValue().setValue(false);

    }

    }

    }



    private void markBasesFromState(MachineState state, Map<GdlSentence, Proposition> bases){

    Set<GdlSentence> contents = state.getContents();

    for(Map.Entry<GdlSentence, Proposition> base : bases.entrySet()){

    if(contents.contains(base.getKey())){

    base.getValue().setValue(true);

    }

    else {

    base.getValue().setValue(false);

    }

    }

    }



    /**

     * Computes if the state is terminal. Should return the value

     * of the terminal proposition for the state.

     */

    @Override

    public boolean isTerminal(MachineState state) {

        if(state == null) {

        return false;

        }

        markBasesFromState(state);

        try {

        return backwardsReason(propNet.getTerminalProposition());

        } catch (Exception e){

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

     * @throws GoalDefinitionException

     */

    @Override

    public int getGoal(MachineState state, Role role) throws GoalDefinitionException {

        markBasesFromState(state);

        Set<Proposition> goals = propNet.getGoalPropositions().get(role);

        GdlSentence sentence = null;

        boolean set = false;

        for(Proposition goal : goals){

        try{

        if(backwardsReason(goal)) {

        sentence = goal.getName();

        if(set) {

        throw new GoalDefinitionException(state, role);

        }

        set = true;

        }

        } catch(Exception e){

        e.printStackTrace();

        throw new GoalDefinitionException(state, role);

        }

        }



        if(sentence == null) {

        throw new GoalDefinitionException(state, role);

        }

        return getGoalValue(sentence);

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

        List<Move> moves = new ArrayList<Move>();

        for(Proposition prop : props){

        moves.add(new Move(prop.getName().get(1)));

        }



        return moves;

    }



    private boolean backwardsReason(Proposition prop) throws Exception{

    Set<Component> sources = prop.getInputs();



    if(sources.size() == 1 && sources.iterator().next() instanceof Transition) return prop.getValue();

    else if(sources.size() == 0) return prop.getValue();



    if(sources.size() == 1){

    Component source = sources.iterator().next();

    return backwardsReasonWrapper(source);

    }



    if(prop.equals(propNet.getInitProposition())) return false;

    return false;

    }



    private boolean backwardsReasonWrapper(Component comp) throws Exception{

    if(comp instanceof Proposition){

    try{

    return backwardsReason((Proposition) comp);



    } catch(Exception e){

    e.printStackTrace();

    return false;

    }

    }

    else {

    try{

    return backwardsConnectiveReason((Component) comp);



    } catch(Exception e){

    e.printStackTrace();

    return false;

    }

    }

    }



    private boolean backwardsConnectiveReason(Component connective) throws Exception{

    Set<Component> inputs = connective.getInputs();

    if(connective instanceof And){

    for(Component input : inputs){

    if(!backwardsReasonWrapper(input)) return false;

    }

    return true;

    } else if(connective instanceof Or){

    for(Component input : inputs) {

    if(backwardsReasonWrapper(input)) return true;

    }

    return false;

    } else if(connective instanceof Not){

    if(inputs.size() != 1) throw new Exception("bad input");

    return !backwardsReasonWrapper(inputs.iterator().next());

    } else if(connective instanceof Constant){

    return connective.getValue();

    } else {



    boolean trans = connective instanceof Transition;

    boolean prop = connective instanceof Proposition;

    throw new Exception("exception");

    }

    }



    @Override

public List<Move> getLegalMoves(MachineState state, Role role) {

    markBasesFromState(state);

    Set<Proposition> props = propNet.getLegalPropositions().get(role);

    List<Move> moves = new ArrayList<Move>();

    for(Proposition prop : props) {

    try {

    if(backwardsReason(prop)) {

    moves.add(new Move(prop.getName().get(1)));

    }

    } catch(Exception e) {

    e.printStackTrace();

    }

    }

    if(moves.size() == 0) System.out.println("Could not find any moves");

    return moves;

    }



    @Override

public MachineState getNextState(MachineState state, List<Move> moves){

    Map<GdlSentence, Proposition> inputs = propNet.getInputPropositions();

    for(Proposition input : inputs.values()){

    input.setValue(false);

    }

    List<GdlSentence> sentences = toDoes(moves);

    for(GdlSentence sent : sentences){

    inputs.get(sent).setValue(true);

    }

    Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();

    markBasesFromState(state, bases);



    Set<GdlSentence> newContents = new HashSet<GdlSentence>();

    for(Proposition base : bases.values()){

    try {

if(backwardsReasonWrapper(base.getSingleInput().getSingleInput())) newContents.add(base.getName());

} catch (Exception e) {

e.printStackTrace();

}

    }



    MachineState newState = new MachineState(newContents);

    return newState;

    }



//    private static List<GdlSentence> toDoes(List<Move> moves) {

//    List<GdlSentence> gdl_proto = new ArrayList<GdlSentence>(moves.size());

//    Map<Role, Integer> roleIndices = getRoleIndices();

//    for(int i = 0; i < roles.size(); i++) {

//    int index = roleIndices.get(roles.get(i));

////    size = gdl_proto();

//    gdl_proto.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));

//    }

//

//    return doeses;

//    }



    /**

     * Computes the legal moves for role in state.

     */

    @Override

//    public List<Move> getLegalMoves(MachineState state, Role role)

//            throws MoveDefinitionException {

//    markBases(state);

//    List<Role> roles = propNet.getRoles();

//    Set<Proposition> legals = new HashSet<Proposition>();

//

//    for (int i = 0; i < roles.size(); i++) {

//    if (role.equals(roles.get(i))) legals = propNet.getLegalPropositions().get(roles.get(i));

//    }

//

//    List<Move> actions = new ArrayList<Move>();

//

//    for (Proposition p : legals) {

//    if (propmarkp(p)) actions.get(actions.size()).equals(p);

//    }

//

//        return actions;

//    }



//    public List<Move> propnext(List<Move> move, MachineState state) {

//    return null;

//    }

//

//    public int propreward(MachineState state, Role role) {

//    return 0;

//    }

//

//    public boolean propterminalp(MachineState state) {

//    return false;

//    }

//

//	private boolean propmarkp(Proposition p) {

//    return true;

//    }

//

//	private boolean propmarknegation(Proposition p) {

//	return !propmarkp(p.getSingleInput());

//	}

//

//	private boolean propmarkconjunction(Proposition p) {

//	return true;

//	}

//

//	private boolean propmarkdisjunction(Proposition p) {

//	return false;

//	}

//

//

//    private boolean markBases(MachineState state) {

//    for (GdlSentence s : propNet.getBasePropositions().keySet()) {

//    if (state.getContents().contains(s)) propNet.getBasePropositions().get(s).setValue(true);

//     else propNet.getBasePropositions().get(s).setValue(false);

//    }

//    return true;

//    }

//

//    private boolean markActions(MachineState state) {

//    return true;

//    }

//

//    private boolean clearPropnet(MachineState state) {

//    return true;

//    }

//

//    /**

//     * Computes the next state given state and the list of moves.

//     */

//    @Override

//    public MachineState getNextState(MachineState state, List<Move> moves)

//            throws TransitionDefinitionException {

//        // TODO: Compute the next state.

//        return null;

//    }



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

    /* Already implemented for you */

    //@Override

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

    private int getGoalValue(GdlSentence sent)

    {

        GdlRelation relation = (GdlRelation) sent;

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