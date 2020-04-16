package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.*;

import java.util.ArrayList;
import java.util.Map;

public class Biu5Behavior extends IAGOCoreBehavior implements BehaviorPolicy {
	
	/*
	 * TODO - will we always have 4 items? because some of the offer code relies on that
	 * What happens to our board when user sends an offer? we need to see that we are using the allocated 
	 * offer correctly, and don't have weird bugs there.
	 */

    private AgentUtilsExtension utils;
    private GameSpec game;
    private Map<String, Integer> payoff;
    private int adverseEvents = 0;
    private Offer allocated;
    private Offer concession; //not sure we need this
    
    //strategy related members
    private boolean userSharePreference = false; //user told us their LWI
    private boolean isUserCooperative = false; //user is being uncooperative (didn't tell us preference)
    private boolean userLeastIsOurMost = false;
    private boolean firstOfferGenerosity = false; //represents if we took least in first offer, if both LW item is the same
    private boolean firstOfferMade = false;
    private ArrayList<Integer> myPreferences;
    
    
    //our different offers, based on user, one or more of these will be suggested during the game
    private Offer bestCaseOffer; //least item is told and its not the same
    private Offer compromiseOffer; //least item is told and its not the same
    private Offer uncooperativeOffer; //user is uncooperative
    private Offer lastRecursiveOffer;
    
    private boolean inRecursiveMode = false;
    //Our recursive strategy will have to be recomputed every time again, since we've passed the first round of
    // offers, and from then on we will use the same strategy to recompute the offer, taking one off and adding one.

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

		//initializing our preference array, index 0 is our most wanted item, whos value is position in the board game.
		this.myPreferences = new ArrayList<>(utils.getMyOrdering());
    }
    
    @Override
	protected void updateAllocated (Offer update)
	{
		allocated = update;
	}
    
    protected void setUserCooperative(boolean cooperative) {
    	this.isUserCooperative = cooperative;
    }

    @Override
	protected Offer getAllocated ()
	{
		return allocated;
	}

    protected boolean getFirstOfferGenerosity() {
    	return this.firstOfferGenerosity;
    }
    
    protected boolean getWasFirstOfferMade() {
    	return this.firstOfferMade;
    }
    @Override
	protected int getAcceptMargin() {
		return Math.max(0, Math.min(game.getNumIssues(), adverseEvents));//basic decaying will, starts with fair
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
    
    /*
     * Following function represents callable offer functions from the coreVH
     */
    
    //function that is called after preferences, while final offers can be called from nextOffer
    protected Offer offer_after_a_preference(History history)
	{
    	int userLW = this.utils.opponent.get_least();
    	int user2LW = this.utils.opponent.get_second_least();
    	
    	if (userLW == -1 && user2LW == -1) {
    		return getUncooperativeOffer(history); //maybe not what we want to do but for now.
    	} else if (userLW != -1 && user2LW != -1) {
    		return getSecondLeastOffer(history, userLW, user2LW);
    	} else if (user2LW == -1 && userLW != -1) {
    		return getFirstLeastOffer(history, userLW);
    	}
    	return null;
	}
    
    @Override
    public Offer getNextOffer(History history) {
    	return null;
    }
    
    @Override
    protected Offer getRejectOfferFollowup(History history) {
        return getNextOffer(history);
    }

    @Override
    protected Offer getFinalOffer(History history) {
        return null;
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
    
    /*
     * Following private section represents all of our different offers
     */
    
    private Offer getFirstLeastOffer(History history, int userLW) {
    	ServletUtils.log("DEBUG - First Cycle Offer, user told least wanted item", ServletUtils.DebugLevels.DEBUG);
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status

    	int myLW = this.myPreferences.get(3); //my least wanted
    	int myMW = this.myPreferences.get(0); //most wanted
    	//two cases:
    	if (myLW == userLW) {
    		ServletUtils.log("DEBUG - First Cycle Offer, LW item is the same", ServletUtils.DebugLevels.DEBUG);
        	
    		//lets divide the least wanted item

    		int ourLeastAmount, userLeastAmount = 0;
    		
    		if (free[userLW] < 5) {
    			ourLeastAmount = free[userLW];
    			userLeastAmount = 0;
    		} else {
    			userLeastAmount = free[userLW] / 2; //should return 2;
    			ourLeastAmount = free[userLW] - userLeastAmount; //should be 3;
    		}
    		if (ourLeastAmount > userLeastAmount) firstOfferGenerosity = true; //first round we are generous;
    		
    		propose.setItem(userLW, new int[] {allocated.getItem(userLW)[0] + ourLeastAmount, 0, allocated.getItem(userLW)[2] + userLeastAmount});
        	
    		this.firstOfferMade = true;
    		this.allocated = propose; //updating our board;
        	return propose;

    	} else {
    		ServletUtils.log("DEBUG - First Cycle Offer, LW item is different", ServletUtils.DebugLevels.DEBUG);
        	
    		if (userLW == myMW) userLeastIsOurMost = true;//great! best case scenario for us.
    		//we'll take user's least, and give our least.
    		
    		//Taking opponent LW item.  users LW column should be now [5,0,0], or otherwise whatever was in the allocated before, except middle is now zero.
        	propose.setItem(userLW, new int[] {allocated.getItem(userLW)[0] + free[userLW], 0, allocated.getItem(userLW)[2]});
        	
        	//Giving our least wanted item
        	propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0], 0, allocated.getItem(myLW)[2] + free[myLW]});
        	
        	this.firstOfferMade = true;
        	this.allocated = propose; //updating our board;
        	return propose;
        	
    	}
    }
    
    private Offer getSecondLeastOffer(History history, int userLW, int user2LW) {
    	ServletUtils.log("DEBUG - Second Cycle Offer, user told second least wanted item", ServletUtils.DebugLevels.DEBUG);
    	
    	return null;
    }
   
    
    //This will be called when we want to make final offer, where we take 
    private Offer getBestCaseOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Best Case Offer", ServletUtils.DebugLevels.DEBUG);

    	/*
    	if (userLeastWantedItem == -1) {
    		ServletUtils.log("ERROR - creating Best Offer, without users least wanted item being set", ServletUtils.DebugLevels.DEBUG);
    		return null;
    	}
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences.get(0); //most wanted
    	int myLW = this.myPreferences.get(3); //least wanted
    	int mySMW = this.myPreferences.get(1); //second most wanted
    	int mySLW = this.myPreferences.get(2); //second least wanted
    	
    	//setting out most wanted item. MW column should be now [5, 0, 0], or otherwise whatever was in allocated before, except middle is now zero.
    	propose.setItem(myMW, new int[] {allocated.getItem(myMW)[0] + free[myMW], 0, allocated.getItem(myMW)[2]});
    	
    	//Taking opponent LW item.  users LW colum should be now [5,0,0], or otherwise whatever was in the allocated before, except middle is now zero.
    	propose.setItem(userLeastWantedItem, new int[] {allocated.getItem(userLeastWantedItem)[0] + free[userLeastWantedItem], 0, allocated.getItem(userLeastWantedItem)[2]});
    	
    	//giving opponent our middle items
    	propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0], 0, allocated.getItem(myLW)[2] + free[myLW]});
    	
    	//giving the last middle wanted item
    	int lastItem = ((mySMW == userLeastWantedItem) ?  mySLW : mySMW);
    	propose.setItem(myLW, new int[] {allocated.getItem(lastItem)[0], 0, allocated.getItem(lastItem)[2] + free[lastItem]});
    	
    	//TODO - for some reason in the Repeatedfavor, they don't update allocated, maybe because only if the user accepts the offer?
    	//In that case, if the user doesn't accept, we are still continuing from where we left of, therefore we should probably update allocated.
    	this.allocated = propose;
    	
    	return propose;
    	*/
    	return null;
    }
    
    //We take half of our least wanted, and our most wanted, and half of second wanted
    private Offer getCompromiseOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Compromise Offer", ServletUtils.DebugLevels.DEBUG);
    	return null;
    }
    
    //we take our first two most wanted
    private Offer getUncooperativeOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Uncooperative Offer", ServletUtils.DebugLevels.DEBUG);
    	return null;
    }
    
    //wherever we are with our last offer, we take one off from the next valuable item, and take one less valuable item.
    private Offer getRecursiveOffer(History history) {
    	return null;
    }
    

    /*
     * Following sections represents utility functions for creating offers
     */
    //copied from repeatedBehavior
    private Offer getCurrentAcceptedBoard() {
    	//start from where we currently have accepted
		Offer propose = new Offer(game.getNumIssues());
		for(int issue = 0; issue < game.getNumIssues(); issue++)
			propose.setItem(issue, allocated.getItem(issue));
		
		return propose;
    }
    
    private int[] getFreeItemsCount() {
    	// Array representing the middle of the board (undecided items)
		int[] free = new int[game.getNumIssues()];
		
		for(int issue = 0; issue < game.getNumIssues(); issue++)
		{
			free[issue] = allocated.getItem(issue)[1];
		}
		return free;
    }
    

}
