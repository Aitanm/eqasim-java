package org.eqasim.san_francisco.uam.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import net.bhl.matsim.uam.config.UAMConfigGroup;
import net.bhl.matsim.uam.dispatcher.UAMManager;
import net.bhl.matsim.uam.infrastructure.UAMStation;



public class UAMTripConstraint  extends AbstractTripConstraint {
	final private UAMManager manager;
	final private UAMConfigGroup uamConfig;

	private UAMTripConstraint(UAMManager manager, UAMConfigGroup uamConfig) {
		this.manager = manager;
		this.uamConfig = uamConfig;
	}
	
	
	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (mode.equals("uam")) {
			Coord originCoord = trip.getOriginActivity().getCoord();
			Coord destinationCoord = trip.getDestinationActivity().getCoord();

			UAMStation originStation = this.manager.getStations().getNearesUAMStation(originCoord);
			UAMStation destinationStation = this.manager.getStations().getNearesUAMStation(destinationCoord);

			if (originStation == destinationStation)
				return false;

			double access_egress_distance = CoordUtils.calcEuclideanDistance(originCoord,
					originStation.getLocationLink().getCoord())
					+ CoordUtils.calcEuclideanDistance(destinationStation.getLocationLink().getCoord(),
					destinationCoord);

			double crowfly_distance = CoordUtils.calcEuclideanDistance(originCoord, destinationCoord);

			if (access_egress_distance > 0.66 * crowfly_distance)
				return false;

			Collection<UAMStation> stationsOrigin = manager.getStations().spatialStations.getDisk(originCoord.getX(),
					originCoord.getY(), uamConfig.getSearchRadius());

			Collection<UAMStation> stationsDestination = manager.getStations().spatialStations
					.getDisk(destinationCoord.getX(), destinationCoord.getY(), uamConfig.getSearchRadius());

			return stationsOrigin.size() != 0 && stationsDestination.size() != 0;

		}

		return true;
	}


	static public class Factory implements TripConstraintFactory {
		final private UAMManager manager;
		final private UAMConfigGroup uamConfig;

		@Inject
		public Factory(UAMManager manager, UAMConfigGroup uamConfig) {
			this.manager = manager;
			this.uamConfig = uamConfig;
		}
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			return new UAMTripConstraint(manager, uamConfig);
		}
	}
}
