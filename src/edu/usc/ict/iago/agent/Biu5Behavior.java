package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;

import java.util.Map;

public class Biu5Behavior extends IAGOCoreBehavior implements BehaviorPolicy {

    private AgentUtilsExtension utils;
    private GameSpec game;
    private Map<String, Integer> payoff;
    private int adverseEvents = 0;
    private Offer allocated;
    private Offer concession; //not sure we need this
    
    //strategy related members
    private boolean userLeastItemSame = false; //user told us the LWI, and its the same as ours (true) otherwise (false)
    private boolean userSharePreference = false; //user told us their LWI
    private boolean isUserCooperative = false; //user is being uncooperative (didn't tell us preference)
    
    //our different offers, based on user, one or more of these will be suggested during the game
    private Offer bestCaseOffer; //least item is told and its not the same
    private Offer compromiseOffer; //least item is told and its not the same
    private Offer uncooperativeOffer; //user is uncooperative
    private Offer lastRecursiveOffer;
    
    private boolean inRecursiveMode = false;
    //Our recursive strategy will have to be recomputed every time again, since we've passed the first round of
    // offers, and from then on we will use the same strategy to recompute the offer, taking one off and adding one.

    @Override
	protected void updateAllocated (Offer update)
	{
		allocated = update;
	}

    @Override
	protected Offer getAllocated ()
	{
		return allocated;
	}

    @Override
    protected Offer getFinalOffer(History history) {
        return null;
    }

    @Override
    protected void setUtils(AgentUtilsExtension utils) {
        this.utils = utils;
        this.game = this.utils.getSpec(); //game spec object, contains points and items for bargening information
        allocated = new Offer(game.getNumIssues()); //the number of items (i.e. issues) that are available (its 4)
		for(int i = 0; i < game.getNumIssues(); i++)
		{
			int[] init = {0, game.getIssueQuants()[i], 0}; //issueQuants is an array the size of numIssues, contains the number of items for each issue. (5 for each in our case)
			allocated.setItem(i, init);
		}
		
    }

    @Override
    protected Offer getTimingOffer(History history) {
        return null;
    }

    @Override
    protected Offer getAcceptOfferFollowup(History history) {
        return null;
    }

    @Override
    protected Offer getFirstOffer(History history) {
        return null;
    }

    @Override
	protected int getAcceptMargin() {
		return Math.max(0, Math.min(game.getNumIssues(), adverseEvents));//basic decaying will, starts with fair
	}

    @Override
    protected Offer getRejectOfferFollowup(History history) {
        return null;
    }

    @Override
	protected Offer getConceded ()
	{
		return allocated;
	}

    @Override
	protected void updateAdverseEvents (int change)
	{
		adverseEvents = Math.max(0, adverseEvents + change);
	}

    @Override
    public Offer getNextOffer(History history) {
    	//TODO: Need to make sure the member: userLeastItemSame, userSharePreference, isUserCooperative 
    	//are updated in CoreVH before we call offer for the first time
    	
    	//user told us the LWI, and its the same as ours
    	if (inRecursiveMode) {
    		return getRecursiveOffer(history);
    	} else {
	    	if (userLeastItemSame) {
	    		inRecursiveMode = true; //in our strategy diagram, once we have made an initial offer, we go to recursive mode
	    		return getCompromiseOffer(history);
	    	} else if (userSharePreference && isUserCooperative) { //the LWI is not the same as ours
	    		inRecursiveMode = true;
	    		return getBestCaseOffer(history);
	    	} else { //assuming user is uncooperative (one of the member is probably not necessary
	    		inRecursiveMode = true;
	    		return getUncooperativeOffer(history);
	    	}
    	}
    }
    
    //We take user's least wanted, and our most wanted
    private Offer getBestCaseOffer(History history) {
    	return null;
    }
    
    //We take half of our least wanted, and our most wanted, and half of second wanted
    private Offer getCompromiseOffer(History history) {
    	return null;
    }
    
    //we take our first two most wanted
    private Offer getUncooperativeOffer(History history) {
    	return null;
    }
    
    //wherever we are with our last offer, we take one off from the next valuable item, and take one less valuable item.
    private Offer getRecursiveOffer(History history) {
    	if (!isUserCooperative) {
    		//return a new uncooperative offer, based on what we have in the current object: uncooperativeOffer, and update
    		return uncooperativeOffer;
    	} else {
    		//else, recompute recursive offer, based on last recursive offer sent
    		return lastRecursiveOffer;
    	} 
    }
    
    
    //function Ofer added to his code, can be relevant code to preferences
    protected Offer offer_after_a_preference(History history)
	{
		return getNextOffer(history);
	}
}
