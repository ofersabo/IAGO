package edu.usc.ict.iago.agent;

import javax.websocket.Session;

import edu.usc.ict.iago.utils.GameSpec;


/**
 * @author mell
 * 
 */
public class IAGOUrsulaFavorVH extends IAGOCoreVH {

	/**
	 * @author mell
	 * Instantiates a new  VH.
	 *
	 * @param name: agent's name
	 * @param game: gamespec value
	 * @param session: the session
	 */
	public IAGOUrsulaFavorVH(String name, GameSpec game, Session session)
	{
		super("UrsulaFavor", game, session, new RepeatedFavorBehavior(RepeatedFavorBehavior.LedgerBehavior.LIMITED), new RepeatedFavorExpression(), 
				new RepeatedFavorMessage(false, false, RepeatedFavorBehavior.LedgerBehavior.LIMITED));	
		
		super.safeForMultiAgent = true;
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