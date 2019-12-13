package org.eqasim.san_francisco.uam;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.san_francisco.uam.mode_choice.UAMModeAvailability;

public class SfUAMModule extends AbstractEqasimExtension {
	static public final String SF_UAM_MODE_AVAILABILITY_NAME = "SfUAMModeAvailability";
	
	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(SF_UAM_MODE_AVAILABILITY_NAME).to(UAMModeAvailability.class);
	}
}
