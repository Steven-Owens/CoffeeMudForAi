package com.planet_ink.coffee_mud.Locales;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import java.util.*;

public class FrozenPlains extends Plains
{
	public String ID(){return "FrozenPlains";}
	public FrozenPlains()
	{
		super();
		recoverEnvStats();
		domainCondition=Room.CONDITION_COLD;
	}

	public Vector resourceChoices(){return Plains.roomResources;}
}
