package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;
import edu.usc.ict.iago.utils.ServletUtils;

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
    private boolean userLeastItemSame = false; //user told us the LWI, and its the same as ours (true) otherwise (false)
    private boolean userSharePreference = false; //user told us their LWI
    private boolean isUserCooperative = false; //user is being uncooperative (didn't tell us preference)
    private int userLeastWantedItem = -1; //should be set in VHCore to user's LW item index
    private int[] myPreferences;
    
    
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
		
		getPreferencesIndices(); //initializing our preference array, index 0 is our most wanted item, whos value is position in the board game.
		
    }
    
    private void getPreferencesIndices() {
    	ArrayList<Integer> myPref = utils.getMyOrdering();
    	this.myPreferences = (int[]) myPref.clone();
    	
		for(int i  = 0; i < game.getNumIssues(); i++) {
			int currentPref = myPref.get(i); //currentpref is the preference number for item in position i
			this.myPreferences[currentPref] = i; //now index "2" in mypreferences array, represents item of preference 2, whos value is the index position
		}
    }
    
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
    	ServletUtils.log("DEBUG - Creating Best Case Offer", ServletUtils.DebugLevels.DEBUG);

    	if (userLeastWantedItem == -1) {
    		ServletUtils.log("ERROR - creating Best Offer, without users least wanted item being set", ServletUtils.DebugLevels.DEBUG);
    		return null;
    	}
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences[0]; //most wanted
    	int myLW = this.myPreferences[-1]; //least wanted
    	int mySMW = this.myPreferences[1]; //second most wanted
    	int mySLW = this.myPreferences[-2]; //second least wanted
    	
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
    }
    
    //We take half of our least wanted, and our most wanted, and half of second wanted
    private Offer getCompromiseOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Compromise Offer", ServletUtils.DebugLevels.DEBUG);
    	
    	if (userLeastWantedItem == -1) {
    		ServletUtils.log("ERROR - creating Compromise Offer, without users least wanted item being set", ServletUtils.DebugLevels.DEBUG);
    		return null;
    	}
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences[0]; //most wanted
    	int myLW = this.myPreferences[-1]; //least wanted - in this case LW == users least wanted
    	int mySMW = this.myPreferences[1]; //second most wanted
    	int mySLW = this.myPreferences[-2]; //second least wanted
    	
    	if (userLeastWantedItem != myLW) {
    		ServletUtils.log("ERROR - creating Compromise Offer, when user's LW is not equal to ours", ServletUtils.DebugLevels.DEBUG);
    		return null;
    	}
    	
    	return null;
    }
    
    //we take our first two most wanted
    private Offer getUncooperativeOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Uncooperative Offer", ServletUtils.DebugLevels.DEBUG);
    	
    	if (userLeastWantedItem != -1) {
    		ServletUtils.log("ERROR - creating Uncooperative Offer, while knowing user's LW item", ServletUtils.DebugLevels.DEBUG);
    		return null;
    	}
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences[0]; //most wanted
    	int myLW = this.myPreferences[-1]; //least wanted - in this case LW == users least wanted
    	int mySMW = this.myPreferences[1]; //second most wanted
    	int mySLW = this.myPreferences[-2]; //second least wanted
    	
    	
    	return null;
    }
    
    //wherever we are with our last offer, we take one off from the next valuable item, and take one less valuable item.
    private Offer getRecursiveOffer(History history) {
    	if (!isUserCooperative) {
    		//return a new uncooperative offer, based on what we have in the current object: uncooperativeOffer, and update
    		return uncooperativeOffer;
    	} else {
    		//else, recompute recursive offer, based on last recursive offer sent
    		Offer propose = getCurrentAcceptedBoard(); //current board status
        	int[] free = getFreeItemsCount(); //middle row current status
        	
        	int myMW = this.myPreferences[0]; //most wanted
        	int myLW = this.myPreferences[-1]; //least wanted - in this case LW == users least wanted
        	int mySMW = this.myPreferences[1]; //second most wanted
        	int mySLW = this.myPreferences[-2]; //second least wanted
        	
        	
    		return null;
    	} 
    }
    
    
    //function Ofer added to his code, can be relevant code to preferences
    protected Offer offer_after_a_preference(History history)
	{
		return getNextOffer(history);
	}
    
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
