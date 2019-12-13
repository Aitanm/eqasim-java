package org.eqasim.san_francisco.uam.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.san_francisco.mode_choice.SanFranciscoModeAvailability;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.discrete_mode_choice.model.mode_availability.ModeAvailability;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class UAMModeAvailability implements ModeAvailability {
	private final ModeAvailability delegate = new SanFranciscoModeAvailability();
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		modes.add("uam");

		
		return modes;
	}
}
