package de.shadowhunt.sonar.plugins.ignorecode;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.SonarPlugin;

import de.shadowhunt.sonar.plugins.ignorecode.batch.IgnoreMissingCoverageDecorator;
import de.shadowhunt.sonar.plugins.ignorecode.rules.IgnoreViolationsFilter;

/**
 * Register all {@code Extension}s
 */
public class IgnoreCodePlugin extends SonarPlugin {

	@Override
	@SuppressWarnings({ "deprecation", "rawtypes" })
	public List getExtensions() {
		return Arrays.asList(IgnoreViolationsFilter.class, IgnoreMissingCoverageDecorator.class);
	}
}
