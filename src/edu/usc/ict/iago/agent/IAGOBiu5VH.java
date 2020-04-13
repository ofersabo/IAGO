package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.GameSpec;

import javax.websocket.Session;

public class IAGOBiu5VH extends IAGOCoreVH {

    /**
     * Constructor for most VHs used by IAGO.
     *
     * @param name       name of the agent. NOTE: Please give your agent a unique name. Do not copy from the default names, such as "Pinocchio,"
     *                   or use the name of one of the character models. An agent's name and getArtName do not need to match.
     * @param game       the GameSpec that the agent plays in first.
     * @param session    the web session that the agent will be active in.
     */
    public IAGOBiu5VH(String name, GameSpec game, Session session) {
        super("Biu5VH", game, session, new Biu5Behavior(), new RepeatedFavorExpression(),
                new RepeatedFavorMessage(false, false, RepeatedFavorBehavior.LedgerBehavior.NONE));
    }

    @Override
    public String getArtName() {
        return "Rens";
    }

    @Override
    public String agentDescription() {
        return "<h1>Opponent</h1><p>They are excited to begin negotiating!</p>";
    }
}
