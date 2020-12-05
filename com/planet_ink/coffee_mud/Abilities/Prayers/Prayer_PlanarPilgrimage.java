package com.planet_ink.coffee_mud.Abilities.Prayers;
import com.planet_ink.coffee_mud.Abilities.StdAbility;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.CMSecurity.DbgFlag;
import com.planet_ink.coffee_mud.core.MiniJSON.MJSONException;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.core.exceptions.CMException;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.PlanarAbility.PlanarVar;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.ExpertiseLibrary;
import com.planet_ink.coffee_mud.Libraries.interfaces.TimeManager;
import com.planet_ink.coffee_mud.Libraries.interfaces.XMLLibrary;
import com.planet_ink.coffee_mud.Libraries.interfaces.XMLLibrary.XMLTag;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2020-2020 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class Prayer_PlanarPilgrimage extends Prayer
{

	@Override
	public String ID()
	{
		return "Prayer_PlanarPilgrimage";
	}

	private final static String	localizedName	= CMLib.lang().L("Planar Pilgrimmage");

	@Override
	public String name()
	{
		return localizedName;
	}

	@Override
	protected int canAffectCode()
	{
		return 0;
	}

	@Override
	protected int canTargetCode()
	{
		return 0;
	}

	@Override
	protected int overrideMana()
	{
		return Ability.COST_ALL;
	}

	private final static String localizedStaticDisplay = CMLib.lang().L("(Planar Pilgrimage)");

	@Override
	public String displayText()
	{
		return localizedStaticDisplay;
	}

	@Override
	public int classificationCode()
	{
		return Ability.ACODE_PRAYER | Ability.DOMAIN_INFLUENTIAL;
	}

	@Override
	public long flags()
	{
		return Ability.FLAG_NOORDERING|Ability.FLAG_NEUTRAL;
	}

	@Override
	public int abstractQuality()
	{
		return Ability.QUALITY_INDIFFERENT;
	}

	@Override
	public void unInvoke()
	{
		if((quest1 != null)
		&&(quest1.running()
			||(CMLib.quests().fetchQuest(quest1.name())==quest1)))
		{
			quest1.stopQuest();
			quest1.enterDormantState();
			CMLib.quests().delQuest(quest1);
			if(affected instanceof MOB)
				((MOB)affected).tell("You have failed the planar pilgrimage.");
		}
		else
		if(affected instanceof MOB)
			((MOB)affected).tell("You have failed the planar pilgrimage.");
		super.unInvoke();
	}

	protected final static LimitedTreeSet<String> lastUsed = new LimitedTreeSet<String>(TimeManager.MILI_DAY, 1000, true);


	protected Quest quest1 = null;

	@Override
	public boolean tick(final Tickable ticking, final int tickID)
	{
		if(!super.tick(ticking, tickID))
			return false;
		final Physical affected=this.affected;
		if(!(affected instanceof MOB))
			return tickUninvoke();
		final MOB mob=(MOB)affected;
		if(quest1 == null)
		{
			final Map<String,Object> definedIDs = new Hashtable<String,Object>();
			definedIDs.put("AREA_NAME", mob.location().getArea().Name());
			definedIDs.put("target_level".toUpperCase(), ""+mob.phyStats().level());
			//definedIDs.put("AGGRESSION", "YES");
			//definedIDs.put("target_is_aggressive".toUpperCase(), "YES");
			definedIDs.put("TEMPLATE", "normal");
			definedIDs.put("DEITYNAME", mob.charStats().getWorshipCharID());
			final Quest q1=deviseAndStartQuest(mob, mob, definedIDs);
			if(q1 == null)
			{
				mob.tell(L("The pilgrimage has failed entirely"));
				return tickUninvoke();
			}
			quest1=q1;
			return true;
		}
		else
		{
			if(!quest1.wasWinner(mob.Name()))
			{
				if(!quest1.running())
					return tickUninvoke();
				// not won, but still running, so keep going...
				return true;
			}
			quest1.stopQuest();
			quest1.enterDormantState();
			CMLib.quests().delQuest(quest1);
			mob.tell("You have successfully completed the planar pilgrimage.");
			final MOB invokerM=invoker();
			if(invokerM!=null)
			{
				final Prayer_PlanarPilgrimage realA=(Prayer_PlanarPilgrimage)invokerM.fetchAbility(ID());
				if(realA!=null)
					lastUsed.add(mob.Name());
			}
			mob.delEffect(this);
			this.setAffectedOne(null);
			return tickUninvoke();
		}
	}

	protected boolean tickUninvoke()
	{
		unInvoke();
		return false;
	}

	@Override
	public void setMiscText(final String newMiscText)
	{
		super.setMiscText(newMiscText);
	}

	public Quest deviseAndStartQuest(final MOB mob, final MOB targetM, final Map<String,Object> definedIDs)
	{
		final Map<String,Object> origDefined=new XHashtable<String,Object>(definedIDs);
		int maxAttempts=5;
		while((--maxAttempts)>=0)
		{
			try
			{
				definedIDs.clear();
				definedIDs.putAll(origDefined);
				final StringBuffer xml = Resources.getFileResource("randareas/example.xml", true);
				if((xml==null)||(xml.length()==0))
				{
					Log.errOut("Unable to generate a quest for "+targetM.name()+" because file not found: randareas/example.xml");
					return null;
				}
				final List<XMLLibrary.XMLTag> xmlRoot = CMLib.xml().parseAllXML(xml);
				if(!definedIDs.containsKey("QUEST_CRITERIA"))
					definedIDs.put("QUEST_CRITERIA", "-NAME \"+"+targetM.Name()+"\" -NPC");
				definedIDs.put("DURATION", ""+CMProps.getTicksPerHour());
				definedIDs.put("EXPIRATION", ""+CMProps.getTicksPerHour());
				definedIDs.put("MULTIAREA", "YES");
				final Map<String,Object> preDefined=new XHashtable<String,Object>(definedIDs);
				CMLib.percolator().buildDefinedIDSet(xmlRoot,definedIDs, preDefined.keySet());
				final String idName = "ALL_QUESTS";
				if((!(definedIDs.get(idName) instanceof XMLTag))
				||(!((XMLTag)definedIDs.get(idName)).tag().equalsIgnoreCase("quest")))
				{
					Log.errOut(L("The quest id '@x1' has not been defined in the data file for @x2.",idName,targetM.name()));
					return null;
				}
				final XMLTag piece=(XMLTag)definedIDs.get(idName);
				try
				{
					CMLib.percolator().checkRequirements(piece, definedIDs);
				}
				catch(final CMException cme)
				{
					Log.errOut(L("Required ids for @x1 were missing: @x2: for @x3",idName,cme.getMessage(),targetM.name()));
					return null;
				}
				String s=CMLib.percolator().buildQuestScript(piece, definedIDs, mob);
				if(s.length()==0)
					throw new CMException("Failed to create any sort of quest at all! WTF!!");
				CMLib.percolator().postProcess(definedIDs);
				if((!definedIDs.containsKey("QUEST_ID"))
				||(!(definedIDs.get("QUEST_ID") instanceof String)))
					throw new CMException("Unable to create your quest because a quest_id was not generated");
				final Quest Q=(Quest)CMClass.getCommon("DefaultQuest");
				s=CMStrings.replaceFirst(s, "set mob reselect", "set pcmob reselect");
				s=CMStrings.replaceFirst(s, "give script LOAD=", "give script PLAYEROK LOAD=");
				Q.setScript(s,true);
				if((Q.name().trim().length()==0)||(Q.duration()<0))
					throw new CMException("Unable to create your quest.  Please consult the log.");
				//mob.tell("Generated quest '"+Q.name()+"'");
				final Quest badQ=CMLib.quests().fetchQuest(Q.name());
				if(badQ!=null)
					throw new CMException("Unable to create your quest.  One of that name already exists!");
				Log.sysOut("Generate",targetM.Name()+" created quest '"+Q.name()+"' via "+ID());
				CMLib.quests().addQuest(Q);
				if(!Q.running())
				{
					if(!Q.startQuest())
					{
						CMLib.quests().delQuest(Q);
						throw new CMException("Unable to start the quest.  Something went wrong.  Perhaps the problem was logged?");
					}
				}
				Q.setCopy(true);
				final CMMsg msg=CMClass.getMsg(targetM, mob.location(),null, CMMsg.MSG_ENTER, null);
				mob.location().send(targetM, msg);
				return Q;
			}
			catch(final CMException cme)
			{
				if(Log.debugChannelOn() && CMSecurity.isDebugging(DbgFlag.MUDPERCOLATOR))
					Log.debugOut(cme);
				else
					Log.errOut(cme.getMessage());
			}
		}
		Log.errOut(L("Failed to finish creating a quest for @x1",targetM.name()));
		return null;
	}

	protected int getXPCost(final Ability imbuePrayerA)
	{
		int experienceToLose=1000;
		experienceToLose+=(100*CMLib.ableMapper().lowestQualifyingLevel(imbuePrayerA.ID()));
		return experienceToLose;
	}

	protected boolean checkAlignment(final MOB mob, final Physical target, final boolean quiet)
	{
		return true;
	}

	protected int maxPrayerLevel()
	{
		return 15;
	}

	private enum QuestTemplate
	{
		PEACEFUL_CAPTURE,
		COLLECT_GROUND,
		COLLECT_MOBS,
		COLLECT_RESOURCES,
		DELIVERY,
		DISPEL,
		ESCORT,
		KILL_ELITE,
		KILL_OFFICER,
		TRAVEL
	}
	
	/*
SKILL	Divine Pilgrimage
Domain	Influencial
Available	Reliquist (35 Q)
Requires	Not learned on Prime Material Plane.
Allows:	Influencing
Use Cost	Max Mana
Quality	Sometimes Beneficial
Targets	MOBS
Range	Touch or Not applicable
Commands	PILGRIMAGE
Usage	PILGRAMAGE [TARGET NAME]
Example	Pilgrimage Garath
Description	A Reliquist may assign a pilgrimage to another follower of their deity or themselves, once per MUDMONTH.  A Reliquist may only assign 1 pilgrimage per 10 levels of Reliquest class, but each target may only participate in 1 pilgramage per mudmoth.  The target will gain a quest to travel to a particular area and plane of existence, and perform a specific task while there.  Upon completion of the task, the target should return to the Reliquist to receive a powerful blessing from the deity as a reward, as well as a significant improvement in their relationship with their deity.
Builder�s Notes	This ability should generate a quest with the Reliquist as the questgiver and rewardgiver.  If the Reliquist cannot accept and/or turn in quests with himself, then remove the ability for the Reliquist to target himself with this ability (and the words in blue above).  The quests assigned will be non-competitive quests, and the planes assigned will be based on the randomly selected quest, the deity, and the target�s alignment/inclination:
Definitions:
�	Same=Within the same alignment/inclination range (PURE GOOD is not the same as GOOD, PURE EVIL is not the same as EVIL, but SOMEWHATGOOD/NEUTRAL/SOMEWHAT evil are all the SAME as Neutral)
�	Similar=within the same alignment/inclination grouping (PURE GOOD or GOOD)/(SOMEWHATGOOD or NEUTRAL or SOMEWHATEVIL)/(EVIL or PUREEVIL) or inclination.
�	Dissimilar=not within the same alignment/inclination grouping (so if the player has a GOOD alignment, the plane would need to be Neutral, Evil or Pure Evil to be dissimilar.
�	Opposed=Opposite alignment/inclination.  PURE GOOD opposes PURE EVIL, GOOD opposes EVIL, Neutral (and sub-cats) opposes itself.  PURE LAW opposes PURE CHAOS, LAW opposes CHAOS, Moderate (and sub-cats) opposes itself.
Quest types:
1.	PEACEFUL CAPTURE-Based off Capture1, must travel to a plane of same or similar alignment to deity and alignment and the same or similar inclination of the target and meet with said creature, who will follow the target back to the Reliquist to turn in the quest.
2.	DEFEAT CAPTURE-Based off Capture1, must travel to a plane of dissimilar alignment to both the deity and the target, and dissimilar inclination to the target.  Defeat the mob in combat to subdue it and then return to the Reliquist to turn in the quest.
3.	COLLECT FROM GROUND-Based on Collect1, must travel to any randomly selected outer plane and gather items.  Items should be called �fragment of [deity name]�, a genitem  that represents a part of a soul faithful to the deity sent to that plane after their death.
4.	COLLECT FROM MOBS-Based off of Collect2, must travel to an outer plane of dissimilar alignment to both the deity and target, and dissimilar inclination to target.
5.	COLLECT RESOURCES-Based off of Collect4, must travel to an inner plane and gather specified resources there (resource should be selected after the plane is selected, and should be among the available resource in the area and gatherable by an available skill the target has).
6.	DELIVERY-Based off of Delivery1, must travel to an outer plane of same or similar alignment to deity and alignment and the same or similar inclination of the target and delivery a GenReadable called �Divine Decree� to the target (remember, the target will be a mob modified by planar descriptions, so we need to make sure one of them exists).
7.	DISPEL1-Based off of Dispel1, must travel to a plane of same or similar alignment to deity and alignment and the same or similar inclination of the target and remove a spell, disease, poison or affect from 1 or more affected mobs.
8.	ESCORT1-Based off of Escort1, must travel to an outer plane of same alignment as deity and target, and same inclination as target with a mob generated from the specified area/plane.  Groups of 2 enemy mobs from the same area opposed plane will attack the escort along the way, spawning every 15-60 ticks.  If the escort dies, the quest is failed.
9.	KILL SOLDIERS-Based off of Killer1, must travel to an outer plane of dissimilar alignment as deity and target, and dissimilar inclination as target and kill 2?10 inhabitants of that area.
10.	KILLER OFFICERS- Based off of Killer1, must travel to an outer plane of dissimilar alignment as deity and target, and dissimilar inclination as target and kill an elite inhabitant of that area.
11.	TRAVEL-Based off of Travel1, must travel to any random plane, random room from selected area, and then return to Reliquist for reward.
If a deity�s alignment is dissimilar to the target�s alignment, then any quest that requires similar or same deity/target alignments should be excluded from the process (1,6,7,8)
Reward for completing quest is 1000 faction with deity, and a buff, FAVORED OF (DEITY NAME) which provides +2 to all stats and +10 hp per level of target, +5 mana per level of target, +5 movement per level of target for the next MUDMONTH.
Expertise should allow the Reliquist to use this ability 1 extra time per mudmonth, but not on the same target.  (I image some sort of tattoo or affect which will preclude a player from participating in subsequent pilgrimages that mudmonth.  Ideally, this would be linked directly to the specific mudmonth, not to time�but a timed tattoo would be fine, too).
	 */

	@Override
	public boolean invoke(final MOB mob, final List<String> commands, final Physical givenTarget, final boolean auto, final int asLevel)
	{
		final MOB targetM=this.getTarget(mob, commands, null);
		if(targetM==null)
			return false;
		if(targetM==mob)
		{
			//special case
			//mob.tell(L("Your deity will not allow you to send yourself on such a quest."));
			//return false;
		}
		if(!targetM.isPlayer())
		{
			mob.tell(L("@x1 does not look ready for such a task.",targetM.Name()));
			return false;
		}
		if(lastUsed.contains(targetM.Name()))
		{
			mob.tell(L("@x1 must wait a bit before being ready again for such a task.",targetM.Name()));
			return false;
		}

		final String deityName=mob.charStats().getWorshipCharID();
		if((deityName.length()==0)||(mob.charStats().getMyDeity()==null))
		{
			mob.tell(L("You must worship a deity to begin the planar pilgrimage.",targetM.Name()));
			return false;
		}
		if(!targetM.charStats().getWorshipCharID().equals(deityName))
		{
			mob.tell(L("@x1 must worship also worship @x2 to begin the planar pilgrimage.",targetM.Name(),deityName));
			return false;
		}

		String winningPlane="";
		List<String> similarPlanes=new ArrayList<String>();
		List<String> dissimilarPlanes=new ArrayList<String>();
		
		QuestTemplate templateCode=null;
		final Deity deityM=mob.charStats().getMyDeity();
		final PlanarAbility planarA=(PlanarAbility)CMClass.getAbility("StdPlanarAbility");
		for(final String planeID : planarA.getAllPlaneKeys())
		{
			final Map<String,String> planeVars = planarA.getPlanarVars(planeID);
			final String alignNumStr = planeVars.get(PlanarVar.ALIGNMENT.toString());
			if((alignNumStr != null)&&(CMath.isInteger(alignNumStr)))
			{
				final Faction F=CMLib.factions().getFaction(CMLib.factions().getAlignmentID());
				if(F!=null)
				{
					final String rangeName = F.fetchRangeName(CMath.s_int(alignNumStr));
					winningPlane="";//TODO: just to prevent final -- delme
					templateCode=null;
				}
			}
			for(final Enumeration<Faction> f=CMLib.factions().factions();f.hasMoreElements();)
			{
				final Faction F=f.nextElement();
				String facNumStr = planeVars.get(F.factionID());
				if((facNumStr == null)||(!CMath.isInteger(facNumStr)))
					facNumStr = planeVars.get(F.name().toUpperCase());
				if((facNumStr == null)||(!CMath.isInteger(facNumStr)))
					continue;
				if(F.factionID().equals(CMLib.factions().getAlignmentID()))
					continue;
				final String rangeName = F.fetchRangeName(CMath.s_int(facNumStr));
				winningPlane="";//TODO: just to prevent final -- delme
				templateCode=null;
			}
		}

		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		final boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			final CMMsg msg=CMClass.getMsg(mob,targetM,this,verbalCastCode(mob,targetM,auto),L("^S<S-NAME> @x1 to grant <T-NAME> a planar pilgrimmage.^?",super.prayWord(mob),targetM.name()));
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				final Ability effA=beneficialAffect(mob,targetM,asLevel,(int)(CMProps.getTicksPerHour()*2));
				if(effA!=null)
					effA.setMiscText("PLANE=\""+winningPlane+"\" DEITY=\""+deityName+"\" TEMPLATECODE=\""+templateCode+"\"");
			}
		}
		else
		{
			beneficialWordsFizzle(mob,targetM,L("<S-NAME> @x1 on behalf of <T-NAMESELF>, but <S-IS-AREA> not answered.",super.prayWord(mob),targetM.Name()));
		}
		// return whether it worked
		return success;
	}
}