package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.agent.RepeatedFavorBehavior.LedgerBehavior;
import edu.usc.ict.iago.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Random;

public class Biu5Behavior extends IAGOCoreBehavior implements BehaviorPolicy
{
    private AgentUtilsExtension utils;
    private GameSpec game;
    private Map<String, Integer> payoff;
    private int adverseEvents = 0;
    private Offer allocated;
    private Offer concession; //not sure we need this
    
    private double OFFER_THRESH = .9;
    private int bestCaseOfferScore = 0;
    
    //strategy related members
    private boolean userSharePreference = false; //user told us their LWI
    private boolean isUserCooperative = false; //user is being uncooperative (didn't tell us preference)
    private boolean userLeastIsOurMost = false;
    private boolean firstOfferGenerosity = false; //represents if we took least in first offer, if both LW item is the same
    private boolean secondOfferGenerosity = false;
    private boolean firstOfferMade = false;
    private boolean secondOfferMade = false;
    private ArrayList<Integer> myPreferences;
    
    private boolean uncooperativeOfferMade = false;
    
    //our different offers, based on user, one or more of these will be suggested during the game
    private Offer bestCaseOffer = null; //least item is told and its not the same
    private Offer compromiseOffer = null; //least item is told and its not the same
    private Offer uncooperativeOffer = null; //user is uncooperative
    private Offer lastRecursiveOffer = null;
    
    private boolean inRecursiveMode = false;
    private ArrayList<Offer> previousOffersMade = new ArrayList<Offer>(); // holds any past offer made (to be used in the getRecursiveOffer)
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
		getPreferencesIndices();
		calculateBestCaseOffer();
    }
    
    private void getPreferencesIndices() {
    	ArrayList<Integer> myPref = utils.getMyOrdering();	 
    	this.myPreferences = (ArrayList<Integer>) myPref.clone();


		for(int i  = 0; i < game.getNumIssues(); i++) {	
			int currentPref = (myPref.get(i) - 1); //currentpref is the preference number for item in position 
			this.myPreferences.set(currentPref, i); //now index "2" in mypreferences array,
		}
    }
    
    private void calculateBestCaseOffer() {
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences.get(0); //most wanted
    	int mySMW = this.myPreferences.get(1); //second most wanted
    	int myTMW = this.myPreferences.get(1); //second most wanted
    	int myLW = this.myPreferences.get(2); //second least wanted
    	
    	//Our best case offer - take our 2 most wanted items, and give user the other two
    	propose.setItem(myMW, new int[] {allocated.getItem(myMW)[0] + free[myMW], 0, allocated.getItem(myMW)[2]});
    	propose.setItem(mySMW, new int[] {allocated.getItem(mySMW)[0] + free[mySMW], 0, allocated.getItem(mySMW)[2]});
    	
    	propose.setItem(myTMW, new int[] {allocated.getItem(myTMW)[0], 0, allocated.getItem(myTMW)[2]  + free[myTMW]});
    	propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0], 0, allocated.getItem(myLW)[2]  + free[myLW]});
    			
    	bestCaseOfferScore = scoreOffer(propose);
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
    
    protected boolean getSecondOfferGenerosity() {
    	return this.secondOfferGenerosity;
    }
    
    protected boolean getWasFirstOfferMade() {
    	return this.firstOfferMade;
    }
    
    protected boolean getWasSecondOfferMade() {
    	return this.secondOfferMade;
    }
    
    @Override
	protected int getAcceptMargin() {
		return Math.max(0, Math.min(game.getNumIssues(), adverseEvents));//basic decaying will, starts with fair
	}


    @Override
	protected Offer getConceded()
	{
		return allocated;
	}

    @Override
	protected void updateAdverseEvents(int change)
	{
		adverseEvents = Math.max(0, adverseEvents + change);
	}
    
    // compare offers (to avoid offering the same one over and over again)
    protected boolean sameOffers(Offer off1, Offer off2)
    {
    	return false;
    }
    
    // a threshold
    protected double gradeOffer(Offer offer)
    {
    	double score = this.scoreOffer(offer);
    	if (score <= utils.myPresentedBATNA)
    		return 0.;
    	else if (score >= bestCaseOfferScore)
    		return 1.;
    	else
    		return score / (double)bestCaseOfferScore;
    }
    
    protected int scoreOffer(Offer offer)
    {
		int totalPoints = 0;
		for (int index = 0; index < game.getNumIssues(); index++)
		{
			String s = game.getIssuePluralNames()[index];
			totalPoints += offer.getItem(index)[0] * game.getSimplePoints(utils.getID()).get(s);
		}
		return totalPoints;
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
    		return getSecondLeastOffer(history, user2LW);
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
        Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status

    	int myMW = this.myPreferences.get(0); //most wanted
    	int mySMW = this.myPreferences.get(1); //second most wanted
    	int myTMW = this.myPreferences.get(1); //second most wanted
    	int myLW = this.myPreferences.get(2); //second least wanted
    	
		//lets divide the items: {ceil(MW / 2), ceil(SMW / 2), floor(TMW / 2), floor(LW / 2)}, give the rest to the user
    	propose.setItem(myMW, new int[] {allocated.getItem(myMW)[0] + (int)((free[myMW] / 2.0) + .5), 0, allocated.getItem(myMW)[2] + (int)(free[myMW] / 2.0)});
    	propose.setItem(mySMW, new int[] {allocated.getItem(mySMW)[0] + (int)((free[mySMW] / 2.0) + .5), 0, allocated.getItem(mySMW)[2] + (int)(free[mySMW] / 2.0)});
    	propose.setItem(myTMW, new int[] {allocated.getItem(myTMW)[0] + (int)(free[myTMW] / 2.0), 0, allocated.getItem(myTMW)[2] + (int)((free[myTMW] / 2.0) + .5)});
    	propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0] + (int)(free[myLW] / 2.0), 0, allocated.getItem(myLW)[2] + (int)((free[myLW] / 2.0) + .5)});
    	
    	return propose;
    }

    @Override
    protected Offer getTimingOffer(History history) {
        return getUncooperativeOffer(history);
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
        	return propose;
        	
    	}
    }
    
    private Offer getSecondLeastOffer(History history, int user2LW) {
    	ServletUtils.log("DEBUG - Second Cycle Offer, user told second least wanted item", ServletUtils.DebugLevels.DEBUG);
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status

    	int my2LW = this.myPreferences.get(2); //my second least wanted
    	int myMW = this.myPreferences.get(0); //most wanted
    	
    	//two cases:
    	if (my2LW == user2LW) {
    		ServletUtils.log("DEBUG - Second Cycle Offer, 2LW item is the same", ServletUtils.DebugLevels.DEBUG);
        	
    		//lets divide the least wanted item
    		int ourLeastAmount, userLeastAmount = 0;
    		
    		if (free[user2LW] < 5) {
    			ourLeastAmount = free[user2LW];
    			userLeastAmount = 0;
    		} else {
        		if (firstOfferGenerosity) {
        			ourLeastAmount = free[user2LW] / 2; //should return 2;
        			userLeastAmount = free[user2LW] - ourLeastAmount; //should be 3;
        		}else {
        			userLeastAmount = free[user2LW] / 2; //should return 2;
        			ourLeastAmount = free[user2LW] - userLeastAmount; //should be 3;
        		}
    		}
    		
    		if (userLeastAmount > ourLeastAmount) secondOfferGenerosity = true; //first round we are generous;
    		
    		propose.setItem(user2LW, new int[] {allocated.getItem(user2LW)[0] + ourLeastAmount, 0, allocated.getItem(user2LW)[2] + userLeastAmount});
        	

    		this.secondOfferMade = true;
        	return propose;
    		
    	} else {
    		
    		if (user2LW == myMW) userLeastIsOurMost = true;//great! best case scenario for us.
    		//we'll take user's least, and give our least.
    		
    		//Taking opponent LW item.  users LW column should be now [5,0,0], or otherwise whatever was in the allocated before, except middle is now zero.
        	propose.setItem(user2LW, new int[] {allocated.getItem(user2LW)[0] + free[user2LW], 0, allocated.getItem(user2LW)[2]});
        	
        	//Giving our least wanted item
        	propose.setItem(my2LW, new int[] {allocated.getItem(my2LW)[0], 0, allocated.getItem(my2LW)[2] + free[my2LW]});
        	

    		this.secondOfferMade = true;
        	return propose;	
    	}
    }
   
    
    //This will be called when we want to make final offer, where we take 
    private Offer getBestCaseOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Best Case Offer", ServletUtils.DebugLevels.DEBUG);
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences.get(0); //most wanted
    	int myLW = this.myPreferences.get(3); //least wanted
    	int mySMW = this.myPreferences.get(1); //second most wanted
    	int mySLW = this.myPreferences.get(2); //second least wanted
    	
    	//setting out most wanted item. MW column should be now [5, 0, 0], or otherwise whatever was in allocated before, except middle is now zero.
    	propose.setItem(myMW, new int[] {allocated.getItem(myMW)[0] + free[myMW], 0, allocated.getItem(myMW)[2]});
    	propose.setItem(mySLW, new int[] {allocated.getItem(mySLW)[0] + free[mySLW], 0, allocated.getItem(mySLW)[2]});
    	
    	//giving opponent mySMW & mLW
    	propose.setItem(mySMW, new int[] {allocated.getItem(mySMW)[0], 0, allocated.getItem(mySMW)[2] + free[mySMW]});
    	propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0], 0, allocated.getItem(myLW)[2] + free[myLW]});
    	    	
    	return propose;
    }
    
    //We take half of our least wanted, and our most wanted, and half of second wanted
    private Offer getCompromiseOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Compromise Offer", ServletUtils.DebugLevels.DEBUG);
    	return null;
    }
    
    //we take our first two most wanted    
    private Offer getUncooperativeOffer(History history) {
    	ServletUtils.log("DEBUG - Creating Uncooperative Offer", ServletUtils.DebugLevels.DEBUG);
    	
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status
    	int[] free = getFreeItemsCount(); //middle row current status
    	
    	int myMW = this.myPreferences.get(0); //most wanted
    	int mySMW = this.myPreferences.get(1); //second most wanted
    	int myTMW = this.myPreferences.get(1); //second most wanted
    	int myLW = this.myPreferences.get(2); //second least wanted
    	
    	//Our uncooperative offer - take our 2 most wanted items, and give user the other two
    	propose.setItem(myMW, new int[] {allocated.getItem(myMW)[0] + free[myMW], 0, allocated.getItem(myMW)[2]});
    	propose.setItem(mySMW, new int[] {allocated.getItem(mySMW)[0] + free[mySMW], 0, allocated.getItem(mySMW)[2]});
    	
    	propose.setItem(myTMW, new int[] {allocated.getItem(myTMW)[0], 0, allocated.getItem(myTMW)[2]  + free[myTMW]});
    	propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0], 0, allocated.getItem(myLW)[2]  + free[myLW]});
    	
    	this.uncooperativeOfferMade = true;
    	return propose;
    }
    
    //wherever we are with our last offer, we take one off from the next valuable item, and take one less valuable item.
    private Offer getRecursiveOffer(History history) {
    	ServletUtils.log("DEBUG - First Cycle Offer, user told least wanted item", ServletUtils.DebugLevels.DEBUG);
    	
    	Offer propose = getCurrentAcceptedBoard(); //current board status (based on the allocated board)
    	int[] free = getFreeItemsCount(); //middle row current status

    	// switch case on "any" combo of the following preferences in order to generate a general offer
    	int myLW = this.myPreferences.get(3); //my least wanted
    	int mySLW = this.myPreferences.get(2); //my second least wanted
    	int mySMW = this.myPreferences.get(1); //second most wanted
    	int myMW = this.myPreferences.get(0); //most wanted
    	
    	int userLW = this.utils.opponent.get_least();
    	int userSLW = this.utils.opponent.get_second_least();
    	int userSMW = this.utils.opponent.get_second_most();
    	int userMW = this.utils.opponent.get_most();
    	
    	boolean generous = firstOfferGenerosity || secondOfferGenerosity;
    	
    	if (uncooperativeOfferMade)
    	{
        	if (!(lastRecursiveOffer == null)) // take .75(MW + SMW), .25(LW + SLW)
        	{
    			int numItems = generous? (int)(Math.ceil(free[mySMW] * .75)) : (int)(Math.floor(free[mySMW] * .75));
    			propose.setItem(myMW, new int[] {allocated.getItem(myMW)[0] + numItems, 0, allocated.getItem(myMW)[2] + (free[myMW] - numItems)});
    			
    			numItems = generous? (int)(Math.ceil(free[mySMW] * .75)) : (int)(Math.floor(free[mySMW] * .75));
    			propose.setItem(mySMW, new int[] {allocated.getItem(mySMW)[0] + numItems, 0, allocated.getItem(mySMW)[2] + (free[mySMW] - numItems)});
    			
    			numItems = generous? (int)(Math.ceil(free[mySLW] * .25)) : (int)(Math.floor(free[mySLW] * .25));
    			propose.setItem(mySLW, new int[] {allocated.getItem(mySLW)[0] + numItems, 0, allocated.getItem(mySLW)[2] + (free[mySLW] - numItems)});
    			
    			numItems = generous? (int)(Math.ceil(free[myLW] * .25)) : (int)(Math.floor(free[myLW] * .25));
    			propose.setItem(myLW, new int[] {allocated.getItem(myLW)[0] + numItems, 0, allocated.getItem(myLW)[2] + (free[myLW] - numItems)});
        	}
        	else // trade 1 item (random)
        	{
        		do {
        			propose = getCurrentAcceptedBoard(); // original current board status (based on the allocated board)
            		Random rand = new Random();
            		int randItem = rand.nextInt(game.getNumIssues());
            		int nextRandItem = randItem;
            		do {
            			nextRandItem = rand.nextInt(game.getNumIssues());
            		} while (randItem == nextRandItem);
            		
            		propose.setItem(randItem, new int[] {propose.getItem(randItem)[0] + 1, 0, propose.getItem(randItem)[2] - 1});
            		propose.setItem(nextRandItem, new int[] {propose.getItem(nextRandItem)[0] - 1, 0, propose.getItem(nextRandItem)[2] + 1});
        		} while (previousOffersMade.contains(propose)); // Offer already re-implements the 'equals' method (no need for us to do that)
        	}
    	}
    	else // first, we wish to use the uncooperative offer
    	{
    		propose = getUncooperativeOffer(history);
    	}
    	
    	if (gradeOffer(propose) < OFFER_THRESH) {
    		propose = null;
    	}
    	
    	if (propose != null)
    	{
    		lastRecursiveOffer = propose;
    		previousOffersMade.add(propose);
    	}
    	
    	return propose;
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
    
	public int getFullyAllocatedItemsCountInt() {
		return Collections.frequency(Arrays.stream(getFreeItemsCount()).boxed().collect(Collectors.toList()), 0);
	}
}
