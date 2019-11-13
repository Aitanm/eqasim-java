package org.eqasim.san_francisco;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.san_francisco.mode_choice.SanFranciscoModeChoiceModule;
import org.eqasim.san_francisco.uam.components.UAMConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import net.bhl.matsim.uam.config.UAMConfigGroup;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		
		//Set up UAM
		UAMConfigurator.configure(config);
		
		
		EqasimConfigGroup.get(config).setTripAnalysisInterval(5);
		EqasimConfigGroup.get(config).setDistanceUnit(DistanceUnit.foot);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		
		//estimator is to be set on the UAMConfigurator.configure method
		eqasimConfig.setEstimator("walk", "sfWalkEstimator");
		eqasimConfig.setEstimator("pt", "sfPTEstimator");

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		
		//add  uam, dvrp and uamModeChoice modules here via UAMConfigurator
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SanFranciscoModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		// controller.addOverridingModule(new CalibrationModule());
		UAMConfigurator.configureController(controller, cmd,  (UAMConfigGroup) config.getModules().get(UAMConfigGroup.GROUP_NAME), config);
		
		controller.run();
	}
}