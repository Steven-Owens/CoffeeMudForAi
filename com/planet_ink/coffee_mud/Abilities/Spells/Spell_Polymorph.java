package com.planet_ink.coffee_mud.Abilities.Spells;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import java.util.*;

public class Spell_Polymorph extends Spell
{

	Race newRace=null;
	public Spell_Polymorph()
	{
		super();
		myID=this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
		name="Polymorph";

		// what the affected mob sees when they
		// bring up their affected list.
		displayText="(Polymorph)";

		canBeUninvoked=true;
		isAutoinvoked=false;
		quality=Ability.MALICIOUS;

		baseEnvStats().setLevel(15);

		uses=Integer.MAX_VALUE;
		recoverEnvStats();
	}

	public Environmental newInstance()
	{
		return new Spell_Polymorph();
	}
	public int classificationCode()
	{
		return Ability.SPELL|Ability.DOMAIN_TRANSMUTATION;
	}

	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		super.affectEnvStats(affected,affectableStats);
		if(newRace!=null)
		if(affected.name().indexOf(" ")>0)
			affectableStats.setReplacementName("a "+newRace.name()+" called "+affected.name());
		else
			affectableStats.setReplacementName(affected.name()+" the "+newRace.name());
	}
	public void affectCharStats(MOB affected, CharStats affectableStats)
	{
		super.affectCharStats(affected,affectableStats);
		if(newRace!=null)
			affectableStats.setMyRace(newRace);
	}


	public void unInvoke()
	{
		// undo the affects of this spell
		if((affected==null)||(!(affected instanceof MOB)))
			return;
		MOB mob=(MOB)affected;
		super.unInvoke();
		mob.tell("You feel more like yourself again.");
	}



	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto)
	{
		MOB target=this.getTarget(mob,commands,givenTarget);
		if(target==null) return false;

		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto))
			return false;

		boolean success=profficiencyCheck(0,auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.
			invoker=mob;
			FullMsg msg=new FullMsg(mob,target,this,affectType,auto?"":"<S-NAME> form(s) a spell around <T-NAMESELF>.");
			if(mob.location().okAffect(msg))
			{
				mob.location().send(mob,msg);
				if(!msg.wasModified())
				{
					int numRaces=CMClass.races.size();
					newRace=null;
					while(newRace==null)
					{
						int raceNum=(int)Math.round(Math.random()*numRaces);
						if(raceNum<numRaces)
							newRace=(Race)CMClass.races.elementAt(raceNum);
						if((newRace!=null)&&(newRace.ID().equals("StdRace")))
							newRace=null;
					}
					mob.location().show(target,null,Affect.MSG_OK_VISUAL,"<S-NAME> become(s) a "+newRace.name()+"!");
					success=beneficialAffect(mob,target,0);
				}
			}
		}
		else
			return beneficialWordsFizzle(mob,target,"<S-NAME> form(s) a spell around <T-NAMESELF>, but the spell fizzles.");

		// return whether it worked
		return success;
	}
}