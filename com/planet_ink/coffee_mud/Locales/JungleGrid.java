package com.planet_ink.coffee_mud.Locales;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import java.util.*;

public class JungleGrid extends StdGrid
{
	public String ID(){return "JungleGrid";}
	public JungleGrid()
	{
		super();
		name="the jungle";
		baseEnvStats.setWeight(3);
		recoverEnvStats();
		domainType=Room.DOMAIN_OUTDOORS_JUNGLE;
		domainCondition=Room.CONDITION_HOT;
	}

	public String getChildLocaleID(){return "Jungle";}
	public Vector resourceChoices(){return Jungle.roomResources;}
}
