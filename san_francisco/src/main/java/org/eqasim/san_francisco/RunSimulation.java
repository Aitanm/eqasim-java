package org.eqasim.san_francisco;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.calibration.CalibrationModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.san_francisco.mode_choice.SanFranciscoModeChoiceModule;
import org.eqasim.san_francisco.uam.SfUAMModule;
import org.eqasim.san_francisco.uam.components.UAMConfigurator;
import org.eqasim.san_francisco.uam.mode_choice.UAMModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.av.framework.AVQSimModule;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import net.bhl.matsim.uam.config.UAMConfigGroup;
import net.bhl.matsim.uam.qsim.UAMQsimModule;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		
		//Added
		UAMConfigurator.configure(config); // Add configuration for UAM
		
		
		EqasimConfigGroup.get(config).setTripAnalysisInterval(5);
		EqasimConfigGroup.get(config).setDistanceUnit(DistanceUnit.foot);
		cmd.applyConfiguration(config);
		
		//Added
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		dmcConfig.setModeAvailability(SfUAMModule.SF_UAM_MODE_AVAILABILITY_NAME);
		

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "sfWalkEstimator");
		eqasimConfig.setEstimator("pt", "sfPTEstimator");

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new SanFranciscoModeChoiceModule(cmd));

//		controller.addOverridingModule(new CalibrationModule());
		//Added
		UAMConfigurator.configureController(controller, cmd, (UAMConfigGroup) config.getModules().get(UAMConfigGroup.GROUP_NAME), config); // Add some modules for UAM
		controller.addOverridingModule(new SfUAMModule());
		
		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator);
			UAMQsimModule.configureComponents(configurator);
		});
		
		
		controller.run();
	}
}