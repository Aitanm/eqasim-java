package org.eqasim.san_francisco.uam.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.san_francisco.uam.mode_choice.utilities.predictors.UAMPredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class UAMUtilityEstimator implements UtilityEstimator{

	//TODO make sure UAMEstimator doesn't need these 
//	private final ModeParameters generalParameters;
//	private final AvModeParameters avParameters;
	private final UAMPredictor predictor; 
	
	@Inject
	public UAMUtilityEstimator(UAMPredictor predictor) {
		this.predictor = predictor;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
				double utility = predictor.calcMaxUtility(person, trip);

				return utility;
	}

}
