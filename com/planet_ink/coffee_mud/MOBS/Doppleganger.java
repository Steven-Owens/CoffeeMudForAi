package com.planet_ink.coffee_mud.MOBS;

import java.util.*;
import com.planet_ink.coffee_mud.utils.*;
import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
public class Doppleganger extends StdMOB
{
	public String ID(){return "Doppleganger";}
	private MOB mimicing=null;
	private long ticksSinceMimicing=0;

	public Doppleganger()
	{
		super();
		revert();
	}

	private void revert()
	{
		Random randomizer = new Random(System.currentTimeMillis());
		Username="a doppleganger";
		setDescription("A formless biped creature, with wicked black eyes.");
		setDisplayText("A formless biped stands here.");
		setBaseEnvStats(new DefaultEnvStats());
		setBaseCharStats(new DefaultCharStats());
		setBaseState(new DefaultCharState());
		setAlignment(0);
		setMoney(250);
		baseEnvStats.setWeight(100 + Math.abs(randomizer.nextInt() % 101));

		baseCharStats().setStat(CharStats.INTELLIGENCE,10 + Math.abs(randomizer.nextInt() % 6));
		baseCharStats().setStat(CharStats.STRENGTH,12 + Math.abs(randomizer.nextInt() % 6));
		baseCharStats().setStat(CharStats.DEXTERITY,9 + Math.abs(randomizer.nextInt() % 6));

		baseEnvStats().setDamage(7);
		baseEnvStats().setSpeed(2.0);
		baseEnvStats().setAbility(0);
		baseEnvStats().setLevel(6);
		baseEnvStats().setArmor(20);

		baseState.setHitPoints(Dice.roll(baseEnvStats().level(),20,baseEnvStats().level()));

		addBehavior(CMClass.getBehavior("Mobile"));
		addBehavior(CMClass.getBehavior("MudChat"));

		recoverMaxState();
		resetToMaxState();
		recoverEnvStats();
		recoverCharStats();
	}



	public boolean tick(Tickable ticking, int tickID)
	{
		if((!amDead())&&(tickID==MudHost.TICK_MOB))
		{
			if(mimicing!=null)
			{
				ticksSinceMimicing++;
				if(ticksSinceMimicing>500)
				{
					revert();
				}
			}
		}
		return super.tick(ticking,tickID);
	}

	public DeadBody killMeDead(boolean createBody)
	{
		revert();
		return super.killMeDead(createBody);
	}

	public boolean okMessage(Environmental myHost, CMMsg msg)
	{
		if(!super.okMessage(myHost,msg))
			return false;
		if((msg.amITarget(this))&&(Util.bset(msg.targetCode(),CMMsg.MASK_MALICIOUS)))
		{
			if(mimicing!=null)
			{
				if((mimicing.getVictim()!=null)&&(mimicing.getVictim()!=this))
					mimicing=null;
				if((mimicing.location()!=null)&&(mimicing.location()!=location()))
					mimicing=null;
			}
			if((mimicing==null)&&(location()!=null)&&(msg.source()!=null))
			{
				location().show(this,null,CMMsg.MSG_OK_VISUAL,"<S-NAME> take(s) on a new form!");
				mimicing=msg.source();
				Username=mimicing.Name();
				setDisplayText(mimicing.displayText());
				setDescription(mimicing.description());
				setBaseEnvStats(mimicing.baseEnvStats().cloneStats());
				setBaseCharStats(mimicing.baseCharStats().cloneCharStats());
				setBaseState(mimicing.baseState().cloneCharState());
				recoverEnvStats();
				recoverCharStats();
				recoverMaxState();
				resetToMaxState();
				ticksSinceMimicing=0;
			}
		}
		return true;
	}
}