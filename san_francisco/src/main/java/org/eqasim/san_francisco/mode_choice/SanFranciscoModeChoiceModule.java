package org.eqasim.san_francisco.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.san_francisco.mode_choice.costs.SanFranciscoCarCostModel;
import org.eqasim.san_francisco.mode_choice.costs.SanFranciscoPtCostModel;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoCostParameters;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoModeParameters;
import org.eqasim.san_francisco.mode_choice.utilities.estimators.SanFranciscoPTUtilityEstimator;
import org.eqasim.san_francisco.mode_choice.utilities.estimators.SanFranciscoWalkUtilityEstimator;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SanFranciscoModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SanFranciscoModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "SanFranciscoCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SanFranciscoPtCostModel";

	public SanFranciscoModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SanFranciscoModeAvailability.class);

		bind(SanFranciscoPersonPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SanFranciscoCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SanFranciscoPtCostModel.class);
        bindUtilityEstimator("sfPTEstimator").to(SanFranciscoPTUtilityEstimator.class);
        bindUtilityEstimator("sfWalkEstimator").to(SanFranciscoWalkUtilityEstimator.class);
		bind(ModeParameters.class).to(SanFranciscoModeParameters.class);
	}

	@Provides
	@Singleton
	public SanFranciscoModeParameters provideModeChoiceParameters(EqasimConfigGroup config) throws IOException, ConfigurationException {
		SanFranciscoModeParameters parameters = SanFranciscoModeParameters.buildDefault();
		
		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}
		
		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		
		return parameters;
	}

	@Provides
	@Singleton
	public SanFranciscoCostParameters provideCostParameters(EqasimConfigGroup config) {
		SanFranciscoCostParameters parameters = SanFranciscoCostParameters.buildDefault();
		
		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}
		
		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		
		return parameters;
	}
}
