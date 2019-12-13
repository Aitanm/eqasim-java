package org.eqasim.san_francisco.uam.components;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.automated_vehicles.mode_choice.AvModeChoiceModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.routing.DefaultEnrichedTransitRouteFactory;
import org.eqasim.san_francisco.uam.mode_choice.UAMModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.simulation.BaselineTransitModule;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import net.bhl.matsim.uam.config.UAMConfigGroup;
import net.bhl.matsim.uam.dispatcher.UAMManager;
import net.bhl.matsim.uam.infrastructure.UAMStations;
import net.bhl.matsim.uam.infrastructure.readers.UAMXMLReader;
import net.bhl.matsim.uam.modechoice.utils.LongPlanFilter;
import net.bhl.matsim.uam.qsim.UAMSpeedModule;
import net.bhl.matsim.uam.router.UAMModes;
import net.bhl.matsim.uam.run.CustomModule;
import net.bhl.matsim.uam.run.UAMModule;

public class UAMConfigurator {
	private UAMConfigurator() {
		
	}
	
	static public void configure(Config config) {
		//add UAM and DVRP modules
		config.addModule(new UAMConfigGroup());
		config.addModule(new DvrpConfigGroup());
		
		// Set up DiscreteModeChoice
		DiscreteModeChoiceConfigGroup dmcConfig = getOrCreateDiscreteModeChoiceConfigGroup(config);
		
		//Add trip constraints TODO
		Set<String> tripConstraints = new HashSet<>();
		tripConstraints.addAll(dmcConfig.getTripConstraints());
		tripConstraints.add(UAMModeChoiceModule.UAM_TRIP_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		
		// Add UAM to the cached modes
		Set<String> cachedModes = new HashSet<>();
		cachedModes.addAll(dmcConfig.getCachedModes());
		cachedModes.add("uam");
		dmcConfig.setCachedModes(cachedModes);
		
		// Set up MATSim scoring (although we don't really use it - MATSim wants it)
		ModeParams modeParams = new ModeParams(UAMModes.UAM_MODE);
		config.planCalcScore().addModeParams(modeParams);
		
		// Set up Eqasim (add UAM cost model and estimator)
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
	//	eqasimConfig.setCostModel("uam", UAMModeChoiceModule.UAM_COST_MODEL_NAME); //TODO Create class and method
		eqasimConfig.setEstimator("uam", UAMModeChoiceModule.UAM_ESTIMATOR_NAME); //
		
		
		// TODO Set up config group -< here they added this new Config group only to pass the parameters in the mode module, check out if this is needed
	//	EqasimAvConfigGroup.getOrCreate(config);
	}
	
	static public void configureController(Controler controller, CommandLine cmd, UAMConfigGroup UAMConfig, Config config) {
		try {
			cmd.applyConfiguration(config);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
		//TODO checkout possible errors due to the EnrichedTransitRoute differences in the eqasim and uam versions
		
		  controller.getScenario().getPopulation().getFactory().getRouteFactories().
		  setRouteFactory(DefaultEnrichedTransitRoute.class, new
		  DefaultEnrichedTransitRouteFactory()); new LongPlanFilter(8, new
		  StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE))
		  .run(controller.getScenario().getPopulation());
		 
		
		
		// Initiate Urban Air Mobility XML reading and parsing
		Network network = controller.getScenario().getNetwork();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		Set<String> modes = new HashSet<>();
		modes.add(UAMModes.UAM_MODE);
		Network networkUAM = NetworkUtils.createNetwork();
		filter.filter(networkUAM, modes);

		filter = new TransportModeNetworkFilter(network);
		Set<String> modesCar = new HashSet<>();
		modesCar.add(TransportMode.car);
		Network networkCar = NetworkUtils.createNetwork();
		filter.filter(networkCar, modesCar);
		
		// set up the UAM infrastructure
		UAMXMLReader uamReader = new UAMXMLReader(networkUAM);

		uamReader.readFile(ConfigGroup.getInputFileURL(controller.getConfig().getContext(), UAMConfig.getUAM())
				.getPath().replace("%20", " "));
		final UAMStations uamStations = new UAMStations(uamReader.getStations(), network);
		final UAMManager uamManager = new UAMManager(network);
		
		// populate UAMManager
		uamManager.setStations(uamStations);
		uamManager.setVehicles(uamReader.getVehicles());
		
		// sets transit modules in case of simulating/not pT
		controller.getConfig().transit().setUseTransit(UAMConfig.getPtSimulation());
		if (UAMConfig.getPtSimulation()) {
			controller.addOverridingModule(new SwissRailRaptorModule());
			controller.addOverridingModule(new BaselineTransitModule());
		}
		controller.addOverridingModule(new CustomModule());
		controller.addOverridingModule(new UAMModule(uamManager, controller.getScenario(), networkUAM, networkCar, uamReader));
		controller.addOverridingModule(new UAMSpeedModule(uamReader.getMapVehicleVerticalSpeeds(),
				uamReader.getMapVehicleHorizontalSpeeds()));
		controller.addOverridingModule(new DvrpTravelTimeModule());
		
//		controller.addOverridingModule(new DvrpModule()); Av uses this but not UAM -> checkout for possible errors
		controller.addOverridingModule(new UAMModeChoiceModule(uamManager, cmd, controller.getScenario(),networkUAM, networkCar));


	}
	
	
	//This replicates the code from AvConfigurator
	static public DiscreteModeChoiceConfigGroup getOrCreateDiscreteModeChoiceConfigGroup(Config config) {
		DiscreteModeChoiceConfigGroup configGroup = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		if (configGroup == null) {
			configGroup = new DiscreteModeChoiceConfigGroup();
			config.addModule(configGroup);
		}

		return configGroup;
	}
}