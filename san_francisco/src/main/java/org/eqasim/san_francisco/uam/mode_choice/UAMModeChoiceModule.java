package org.eqasim.san_francisco.uam.mode_choice;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.automated_vehicles.mode_choice.constraints.AvWalkConstraint;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.config.TransitConfigGroup;

import net.bhl.matsim.uam.modechoice.estimation.pt.subscription.SubscriptionFinder;
import net.bhl.matsim.uam.modechoice.tracking.TravelTimeTracker;
import net.bhl.matsim.uam.qsim.UAMLinkSpeedCalculator;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;
import net.bhl.matsim.uam.config.UAMConfigGroup;
import net.bhl.matsim.uam.data.UAMStationConnectionGraph;
import net.bhl.matsim.uam.data.WaitingStationData;
import net.bhl.matsim.uam.dispatcher.UAMManager;
import net.bhl.matsim.uam.modechoice.CustomCarDisutility;
import net.bhl.matsim.uam.modechoice.estimation.CustomModeChoiceParameters;

import org.eqasim.san_francisco.uam.mode_choice.constraints.UAMTripConstraint;
import org.eqasim.san_francisco.uam.mode_choice.utilities.estimators.UAMUtilityEstimator;
import org.eqasim.san_francisco.uam.mode_choice.utilities.predictors.UAMPredictor;

public class UAMModeChoiceModule extends AbstractEqasimExtension {
	static public final String UAM_ESTIMATOR_NAME = "UAMEstimator";
	static public final String UAM_COST_MODEL_NAME = "UAMCostModel";
	static public final String UAM_TRIP_CONSTRAINT_NAME = "UAMTripConstraint"; 

	
	private final CommandLine cmd;
	private UAMManager uamManager;
	private Scenario scenario;
	private Network networkUAM;
	private Network networkCar;

	public UAMModeChoiceModule(UAMManager uamManager, CommandLine commandLine, Scenario scenario2, Network networkUAM,
			Network networkCar) {
		this.cmd = commandLine;
		this.uamManager = uamManager;
		this.scenario = scenario;
		this.networkUAM = networkUAM;
		this.networkCar = networkCar;
	}

	@Override
	protected void installEqasimExtension() {
		// TODO create classes
		bindUtilityEstimator(UAM_ESTIMATOR_NAME).to(UAMUtilityEstimator.class);
//		bindCostModel(UAM_COST_MODEL_NAME).to(UAMCostModel.class); //TODO checkout the need to create it
		bind(UAMPredictor.class);
		

		bind(TravelTimeTracker.class);

		addTravelDisutilityFactoryBinding("car").to(CustomCarDisutility.Factory.class);
		// Checkout constraints TODO
		bindTripConstraintFactory(UAM_TRIP_CONSTRAINT_NAME).to(UAMTripConstraint.Factory.class);


	}

	// TODO
	// ADD PROVIDERS HERE 
	@Provides
	@Singleton
	public CustomModeChoiceParameters provideCustomModeChoiceParameters() throws ConfigurationException {
		String prefix = "scoring:";

		List<String> scoringOptions = cmd.getAvailableOptions().stream().filter(o -> o.startsWith(prefix))
				.collect(Collectors.toList());

		Map<String, String> rawParameters = new HashMap<>();

		for (String option : scoringOptions) {
			rawParameters.put(option.substring(prefix.length()), cmd.getOptionStrict(option));
		}

		CustomModeChoiceParameters parameters = new CustomModeChoiceParameters();
		parameters.load(rawParameters);

		return parameters;
	}

	/*
	 * @Provides UAMPredictor provideUAMPredictor(UAMManager manager, Scenario
	 * scenario, WaitingStationData waitingData, UAMConfigGroup uamConfig,
	 * TransitConfigGroup transitConfigGroup, Network carNetwork, TripRouter
	 * transitRouter, CustomModeChoiceParameters parameters,
	 * UAMStationConnectionGraph stationConnectionutilities, SubscriptionFinder
	 * subscriptions, LeastCostPathCalculatorFactory lcpcf, Map<String,
	 * TravelDisutilityFactory> travelDisutilityFactories,Map<String, TravelTime>
	 * travelTimes) {
	 * 
	 * Set<String> modes = uamConfig.getAvailableAccessModes(); TravelTime
	 * travelTime = travelTimes.get(TransportMode.car); TravelDisutility
	 * travelDisutility = travelDisutilityFactories.get(TransportMode.car)
	 * .createTravelDisutility(travelTime); LeastCostPathCalculator plcpccar =
	 * lcpcf.createPathCalculator(networkCar, travelDisutility, travelTime); return
	 * new UAMPredictor(manager, scenario, waitingData, uamConfig,
	 * transitConfigGroup, modes, carNetwork, plcpccar, transitRouter, parameters,
	 * stationConnectionutilities, subscriptions); }
	 */

	@Provides
	@Singleton
	public SubscriptionFinder provideSubscriptionFinder(Population population) {
		return new SubscriptionFinder(population.getPersonAttributes());
	}

	@Provides
	@Named("road")
	@Singleton
	public Network provideRoadNetwork(Network fullNetwork) {
		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(fullNetwork).filter(roadNetwork, Collections.singleton("car"));
		return roadNetwork;
	}

	@Provides
	@Singleton
	public CustomCarDisutility.Factory provideCustomCarDisutilityFactory(UAMLinkSpeedCalculator speedCalculator) {
		return new CustomCarDisutility.Factory(speedCalculator);
	}

	@Provides
	@Singleton
	public UAMStationConnectionGraph provideUAMStationConnectionGraph(UAMManager uamManager,
			CustomModeChoiceParameters parameters, @Named("uam") ParallelLeastCostPathCalculator plcpc) {
		return new UAMStationConnectionGraph(uamManager, parameters, plcpc);
	}

}

