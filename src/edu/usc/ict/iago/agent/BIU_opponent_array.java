package edu.usc.ict.iago.agent;

import java.util.ArrayList;

import edu.usc.ict.iago.utils.Preference;
import edu.usc.ict.iago.utils.Preference.Relation;



public class BIU_opponent_array {
	int vertexCount = 4;
	ArrayList<ArrayList<Integer>> graph = new ArrayList<>(vertexCount);
	
	public BIU_opponent_array() {
		for(int i=0; i < vertexCount; i++) {
		    graph.add(new ArrayList<Integer>());
		}
	}
	
	public void add_pref(Preference pref ) 
	{
		int i1 = pref.getIssue1(); 
		int i2 = pref.getIssue2();
		Preference.Relation r = pref.getRelation();
		if(r == Relation.BEST)
		{
			if (pref.getIssue1() == -1)//if information not filled in
				return;
			
			ArrayList<Integer> thisIssue = graph.get(i1);
			for (int x = 0; x < graph.size(); x++)
			{
				if (i1 == pref.getIssue1())
						continue;
				
				ArrayList<Integer> vertex = graph.get(x);
				if (!vertex.contains(i1) && !thisIssue.contains(x))
					vertex.add(i1);
				
			}
		}
		else if(r == Relation.WORST)
		{
			if (pref.getIssue1() == -1)//if information not filled in
				return;
			for (int x = 0; x < graph.size(); x++)
			{
				if (x == i1)
					continue;
				
				ArrayList<Integer> vertex = graph.get(i1);
				ArrayList<Integer> other = graph.get(x);
				if (!vertex.contains(x) && !other.contains(i1))
					vertex.add(x);
				
			}
		}
		else if(r == Relation.GREATER_THAN)
		{
			//kludge when vague information is supplied
			if (pref.getIssue1() == -1 || pref.getIssue2() == -1)//if information not filled in
				return;
			
			graph.get(i2).add(i1);
			

		}
		else if(r == Relation.LESS_THAN)
		{
			if (pref.getIssue1() == -1 || pref.getIssue2() == -1)//if information not filled in
				return;
			
			graph.get(i1).add(i2);
		}
		else if(r == Relation.EQUAL)
		{
			return;
		}
		
	}
	
	public int get_least() {
		int max = -1;
		int index = -1;
		for (int x = 0; x < graph.size(); x++)
		{	
			int v = graph.get(x).size();
			if (v > max) {
				max = v;
				index = x;
			}
		
			
		}
		if (max == 0)
			return -1;
		
		return index;
	}
	
	public int get_second_least() {
		int max = -999;
		int sec_max = -999;
		int index = -1;
		int first_index = -1;
		for (int x = 0; x < graph.size(); x++)
		{	
			int v = graph.get(x).size();
			if (v > max) {
				sec_max = max;
				index = first_index;
				
				max = v;
				first_index = x;
			}
			else if (v>sec_max) {
				sec_max = v;
				index = x;
			}
					
			
		}
		if (sec_max == 0)
			return -1;
		
		return index;
	}
	
	public int get_second_most() {
		int min = -999;
		int sec_min = -999;
		int index = -1;
		int first_index = -1;
		for (int x = 0; x < graph.size(); x++)
		{	
			int v = graph.get(x).size();
			if (v < min) {
				sec_min = min;
				index = first_index;
				
				min = v;
				first_index = x;
			}
			else if (v < sec_min) {
				sec_min = v;
				index = x;
			}
					
			
		}
		if (sec_min == 0)
			return -1;
		
		return index;
	}
	
	public int get_most() {
		int min = -1;
		int index = -1;
		for (int x = 0; x < graph.size(); x++)
		{	
			int v = graph.get(x).size();
			if (v < min) {
				min = v;
				index = x;
			}
		
			
		}
		if (min == 0)
			return -1;
		
		return index;
	}
}
